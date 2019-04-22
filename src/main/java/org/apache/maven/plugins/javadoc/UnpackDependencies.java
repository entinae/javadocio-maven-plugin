/* Copyright (c) 2019 OpenJAX
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.dependency.AbstractDependencyMojo;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.fromDependencies.UnpackDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

class UnpackDependencies extends UnpackDependenciesMojo {
  static {
    System.setProperty("http.agent", "");
  }

  private final Set<Artifact> resolvedArtifacts = new HashSet<>();
  private final List<OfflineLink> offlineLinks = new ArrayList<>();
  private final Map<String,String> dependencyToUrl;
  private final boolean offline;
  private final MavenProject project;

  UnpackDependencies(final Map<String,String> dependencyToUrl, final Log log, final boolean offline, final MavenProject project, final MavenSession session, final List<ArtifactRepository> remoteRepositories, final List<MavenProject> reactorProjects, final ArchiverManager archiverManager, final ArtifactResolver artifactResolver, final DependencyResolver dependencyResolver, final RepositoryManager repositoryManager, final ProjectBuilder projectBuilder, final ArtifactHandlerManager artifactHandlerManager) {
    setLog(log);
    this.dependencyToUrl = dependencyToUrl;
    this.offline = offline;
    this.project = project;
    this.session = session;
    setField(AbstractDependencyMojo.class, this, "remoteRepositories", remoteRepositories);
    this.reactorProjects = reactorProjects;

    setArchiverManager(archiverManager);
    setField(AbstractDependencyFilterMojo.class, this, "artifactResolver", artifactResolver);
    setField(AbstractDependencyFilterMojo.class, this, "dependencyResolver", dependencyResolver);
    setField(AbstractDependencyFilterMojo.class, this, "repositoryManager", repositoryManager);
    setField(AbstractDependencyFilterMojo.class, this, "projectBuilder", projectBuilder);
    setField(AbstractDependencyFilterMojo.class, this, "artifactHandlerManager", artifactHandlerManager);

    this.failOnMissingClassifierArtifact = false;
    this.classifier = "javadoc";
    this.outputDirectory = new File(getProject().getBuild().getDirectory(), "javadocdep");
    this.useSubDirectoryPerArtifact = true;
  }

  List<OfflineLink> getOfflineLinks() {
    return this.offlineLinks;
  }

  @Override
  public MavenProject getProject() {
    return project;
  }

  private File getDestDir(final Artifact artifact) {
    return DependencyUtil.getFormattedOutputDirectory(useSubDirectoryPerScope, useSubDirectoryPerType, useSubDirectoryPerArtifact, useRepositoryLayout, stripVersion, outputDirectory, artifact);
  }

  private String getUrl(final Artifact artifact) {
    final String url = dependencyToUrl.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
    if (url == null)
      return getModelUrl(new File(artifact.getFile().toString().replace("-javadoc.jar", ".pom"))) + "apidocs/";

    return url.replace("@version", artifact.getVersion());
  }

  private String getCheckUrl(final Artifact artifact) {
    final String url = getUrl(artifact);
    if (!offline && !exists(url))
      getLog().warn("Error fetching link for dependency [" + artifact.getGroupId() + ":" + artifact.getArtifactId() + "]: " + url);

    return url;
  }

  private String getJavadocIoUrl(final Artifact artifact) {
    final String version = artifact.getVersion().replace("-SNAPSHOT", "");
    final String url ="https://static.javadoc.io/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + version;
    if (!offline && !exists(url + "index.html"))
      exists("https://www.javadoc.io/doc/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + version);

    return url;
  }

  @Override
  protected DependencyStatusSets getDependencySets(final boolean stopOnFailure) throws MojoExecutionException {
    final DependencyStatusSets dependencyStatusSets = super.getDependencySets(stopOnFailure);
    for (final Artifact artifact : dependencyStatusSets.getResolvedDependencies()) {
      resolvedArtifacts.add(artifact);

      final OfflineLink offlineLink = new OfflineLink();
      offlineLink.setUrl(getJavadocIoUrl(artifact));
      offlineLink.setLocation(getDestDir(artifact).getAbsolutePath());
      offlineLinks.add(offlineLink);
    }

    return dependencyStatusSets;
  }

  @Override
  protected void doExecute() throws MojoExecutionException {
    super.doExecute();
    try {
      for (final Artifact artifact : resolvedArtifacts)
        checkPackageList(getDestDir(artifact));
    }
    catch (final IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}