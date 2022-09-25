/* Copyright (c) 2019 Seva Safris
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.apache.maven.plugins.javadoc;

import static org.apache.maven.plugins.javadoc.MojoUtil.*;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.dependency.AbstractDependencyMojo;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.fromDependencies.UnpackDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

class UnpackDependencies extends UnpackDependenciesMojo {
  private static final HashMap<Artifact,Set<OfflineLink>> artifactToOfflineLinks = new HashMap<>();
  private static final HashMap<Artifact,OfflineLink> artifactToDependencyLink = new HashMap<>();
  private static final boolean reportError;

  static {
    final String sunJavaCommand = System.getProperty("sun.java.command") + " ";
    reportError = sunJavaCommand.contains(" -e ") || sunJavaCommand.contains(" --error ");
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
    System.setProperty("http.agent", "");
  }

  static ArrayList<OfflineLink> execute(final DefaultMojo mojo, final Settings settings, final MavenProject project, final MavenSession session, final List<MavenProject> reactorProjects, final ArchiverManager archiverManager, final ArtifactResolver artifactResolver, final DependencyResolver dependencyResolver, final RepositoryManager repositoryManager, final ProjectBuilder projectBuilder, final ArtifactHandlerManager artifactHandlerManager) throws MojoExecutionException, MojoFailureException {
    final UnpackDependencies unpackDependencies = new UnpackDependencies(mojo, settings, project, session, reactorProjects, archiverManager, artifactResolver, dependencyResolver, repositoryManager, projectBuilder, artifactHandlerManager);
    unpackDependencies.execute();
    final ArrayList<OfflineLink> offlineLinks = new ArrayList<>(unpackDependencies.offlineLinks);
    // Remove the 1st entry, as it is the entry for this project itself
    offlineLinks.remove(0);
    final Log log = mojo.getLog();
    if (log.isDebugEnabled()) {
      log.debug("Detected offline links...");
      for (int i = 0, i$ = offlineLinks.size(); i < i$; ++i) { // [RA]
        final OfflineLink offlineLink = offlineLinks.get(i);
        for (final String line : offlineLink.toString().split("\n")) // [A]
          log.debug(line);
      }
    }

    return offlineLinks;
  }

  private final DefaultMojo mojo;
  private final Set<OfflineLink> offlineLinks;
  private final boolean offline;
  private final MavenProject project;

  private UnpackDependencies(final DefaultMojo mojo, final Settings settings, final MavenProject project, final MavenSession session, final List<MavenProject> reactorProjects, final ArchiverManager archiverManager, final ArtifactResolver artifactResolver, final DependencyResolver dependencyResolver, final RepositoryManager repositoryManager, final ProjectBuilder projectBuilder, final ArtifactHandlerManager artifactHandlerManager) {
    this.mojo = mojo;
    setLog(new FilterLog(mojo.getLog()) {
      @Override
      public boolean isInfoEnabled() {
        return false;
      }
    });
    this.offlineLinks = addModules(getModelArtifact(new File(project.getBasedir(), "pom.xml")));
    this.offline = settings.isOffline();
    this.project = project;
    this.session = session;
    setField(AbstractDependencyMojo.class, this, "remoteRepositories", project.getRemoteArtifactRepositories());
    this.reactorProjects = reactorProjects;

    setArchiverManager(archiverManager);
    setField(AbstractDependencyFilterMojo.class, this, "artifactResolver", artifactResolver);
    setField(AbstractDependencyFilterMojo.class, this, "dependencyResolver", dependencyResolver);
    setField(AbstractDependencyFilterMojo.class, this, "repositoryManager", repositoryManager);
    setField(AbstractDependencyFilterMojo.class, this, "projectBuilder", projectBuilder);
    setField(AbstractDependencyFilterMojo.class, this, "artifactHandlerManager", artifactHandlerManager);

    this.failOnMissingClassifierArtifact = false;
    this.classifier = "javadoc";
    this.outputDirectory = new File(settings.getLocalRepository());
    this.markersDirectory = new File(settings.getLocalRepository(), "dependency-maven-plugin-markers");
  }

  private Set<OfflineLink> addModules(final Model model) {
    final Artifact artifact = new DefaultArtifact(model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId(), model.getArtifactId(), model.getVersion() != null ? model.getVersion() : model.getParent().getVersion(), "compile", "jar", null, new DefaultArtifactHandler());
    final Set<OfflineLink> links = artifactToOfflineLinks.get(artifact);
    if (links != null)
      return links;

    if ("pom".equalsIgnoreCase(model.getPackaging())) {
      final Set<OfflineLink> moduleLinks = new LinkedHashSet<>();
      for (final String module : model.getModules()) { // [L]
        final File path = new File(model.getProjectDirectory(), module);
        final Model submodule = getModelArtifact(new File(path, "pom.xml"));
        moduleLinks.addAll(addModules(submodule));
      }

      artifactToOfflineLinks.put(artifact, moduleLinks);
      return moduleLinks;
    }

    final OfflineLink offlineLink = new OfflineLink();
    offlineLink.setUrl(getJavadocIoLink(artifact));
    final File apiDocs = new File(model.getPomFile().getParentFile(), "target/" + mojo.getApiDocsTargetPath() + "/");
    offlineLink.setLocation(apiDocs.getAbsolutePath());
    final Set<OfflineLink> moduleLinks = new LinkedHashSet<>();
    moduleLinks.add(offlineLink);
    artifactToOfflineLinks.put(artifact, moduleLinks);
    return moduleLinks;
  }

  @Override
  public MavenProject getProject() {
    return project;
  }

  private static String getJavadocLink(final Artifact artifact) {
    final String filePath = artifact.getFile().toString();
    if (!filePath.endsWith("-javadoc.jar"))
      return null;

    return getModelUrl(new File(filePath.substring(filePath.length() - 12) + ".pom")) + "apidocs/";
  }

  private String getJavadocIoLink(final Artifact artifact) {
    final String version = artifact.getVersion().replace("-SNAPSHOT", "");
    final String url ="https://static.javadoc.io/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + version + "/";
    // Trigger javadoc.io to start downloading the javadocs if not yet available
    if (!offline && !exists(url + "index.html"))
      exists("https://www.javadoc.io/doc/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + version + "/");

    return url;
  }

  private boolean downloadPackageList(String docUrl, final File file) {
    if (docUrl == null)
      return false;

    try {
      final int responseCode = downloadFile(docUrl = docUrl + "package-list", file);
      if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED)
        getLog().debug("Not Modified: " + docUrl);

      return true;
    }
    catch (final IOException e) {
      String message = e.getMessage();
      if (!message.contains(docUrl))
        message += ": " + docUrl;

      if (reportError)
        getLog().warn(message, e);
      else
        getLog().warn(message);

      return false;
    }
  }

  private void addDependency(final Artifact artifact, final boolean resolved) {
    final Set<OfflineLink> offlineLinks = artifactToOfflineLinks.get(artifact);
    if (offlineLinks != null) {
      this.offlineLinks.addAll(offlineLinks);
    }
    else {
      OfflineLink dependencyLink = artifactToDependencyLink.get(artifact);
      if (dependencyLink == null) {
        final File destDir = getFormattedOutputDirectory(artifact);
        dependencyLink = new OfflineLink();
        dependencyLink.setUrl(getJavadocIoLink(artifact));
        dependencyLink.setLocation(destDir.getAbsolutePath());
        if (!resolved) {
          destDir.mkdirs();
          final File packageListFile = new File(destDir, "package-list");
          if (!downloadPackageList(getJavadocIoLink(artifact), packageListFile) && !downloadPackageList(getJavadocLink(artifact), packageListFile)) {
            getLog().error("Unable to resolve dependency: " + artifact.getId());
            return;
          }
        }

        artifactToDependencyLink.put(artifact, dependencyLink);
      }

      this.offlineLinks.add(dependencyLink);
    }
  }

  private File getFormattedOutputDirectory(final Artifact artifact) {
    final StringBuilder builder = new StringBuilder(128);
    builder.append(artifact.getGroupId().replace('.', File.separatorChar)).append(File.separatorChar);
    builder.append(artifact.getArtifactId()).append(File.separatorChar);
    builder.append(artifact.getBaseVersion()).append(File.separatorChar);
    builder.append("javadoc").append(File.separatorChar);
    return new File(outputDirectory, builder.toString());
  }

  @Override
  protected DependencyStatusSets getDependencySets(final boolean stopOnFailure) throws MojoExecutionException {
    final DependencyStatusSets dependencyStatusSets = super.getDependencySets(stopOnFailure);
    for (final Artifact artifact : dependencyStatusSets.getUnResolvedDependencies()) // [S]
      addDependency(artifact, false);

    for (final Artifact artifact : dependencyStatusSets.getResolvedDependencies()) // [S]
      addDependency(artifact, true);

    return dependencyStatusSets;
  }

  @Override
  protected void unpack(final Artifact artifact, final File location, final String includes, final String excludes, final String encoding) throws MojoExecutionException {
    super.unpack(artifact, getFormattedOutputDirectory(artifact), includes, excludes, encoding);
  }

  @Override
  protected void doExecute() throws MojoExecutionException {
    super.doExecute();
    try {
      for (final OfflineLink offlineLink : offlineLinks) // [S]
        checkPackageList(offlineLink.getLocation());
    }
    catch (final IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}