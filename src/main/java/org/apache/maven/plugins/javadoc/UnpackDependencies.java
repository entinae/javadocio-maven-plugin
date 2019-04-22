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

import static org.apache.maven.plugins.javadoc.JavadocDepUtil.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.dependency.AbstractDependencyMojo;
import org.apache.maven.plugins.dependency.fromDependencies.UnpackDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

class UnpackDependencies extends UnpackDependenciesMojo {
  private static String checkSlash(final String url) {
    return url.charAt(url.length() - 1) != '/' ? url + "/" : url;
  }

  private static String getModelUrl(final File pomFile) {
    try {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final Model model = reader.read(new FileReader(pomFile));
      final String url = model.getUrl();
      if (url != null)
        return checkSlash(url);

      final String id = getId(model);
      final String artifactDir = pomFile.getParent();
      final String localRepoPath = artifactDir.substring(0, artifactDir.length() - id.length());
      final String parentUrl = getParentPath(localRepoPath, model.getParent());
      return checkSlash(parentUrl) + model.getArtifactId() + "/";
    }
    catch (final IOException | XmlPullParserException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String getId(final Model model) {
    final String groupId = model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId();
    final String version = model.getVersion() != null ? model.getVersion() : model.getParent().getVersion();
    return groupId + ":" + model.getArtifactId() + ":" + version;
  }

  private static String getParentPath(final String localRepoPath, final Parent parent) {
    final String artifactPath = parent.getGroupId().replace('.', '/') + "/" + parent.getArtifactId() + "/" + parent.getVersion() + "/";
    final String parentPath = localRepoPath + artifactPath;
    return getModelUrl(new File(parentPath, parent.getArtifactId() + "-" + parent.getVersion() + ".pom"));
  }

  private static void checkPackageList(final File destDir) throws IOException {
    final File packageListFile = new File(destDir, "package-list");
    if (!packageListFile.exists()) {
      final File elementListFile = new File(destDir, "element-list");
      if (elementListFile.exists())
        FileUtils.copyFile(elementListFile, packageListFile);
    }
  }

  private static boolean exists(final String url) {
    try {
      new URL(url).openStream().close();
      return true;
    }
    catch (final IOException e) {
      return false;
    }
  }

  private final List<OfflineLink> offlineLinks = new ArrayList<>();
  private final Map<String,String> dependencyToUrl;
  private final MavenProject project;

  UnpackDependencies(final Map<String,String> dependencyToUrl, final Log log, final MavenProject project, final MavenSession session, final List<ArtifactRepository> remoteRepositories, final List<MavenProject> reactorProjects) {
    setLog(log);
    this.dependencyToUrl = dependencyToUrl;
    this.project = project;
    this.session = session;
    setField(AbstractDependencyMojo.class, this, "remoteRepositories", remoteRepositories);
    this.reactorProjects = reactorProjects;

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
    if (!exists(url))
      getLog().warn("Error fetching link for dependency [" + artifact.getGroupId() + ":" + artifact.getArtifactId() + "]: " + url);

    return url;
  }

  private static String getJavadocIoUrl(final Artifact artifact) {
    return "https://www.javadoc.io/doc/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion().replace("-SNAPSHOT", "");
  }

  @Override
  protected DependencyStatusSets getDependencySets(final boolean stopOnFailure) throws MojoExecutionException {
    try {
      final DependencyStatusSets dependencyStatusSets = super.getDependencySets(stopOnFailure);
      for (final Artifact artifact : dependencyStatusSets.getResolvedDependencies()) {
        final File destDir = getDestDir(artifact);
        checkPackageList(destDir);

        final OfflineLink offlineLink = new OfflineLink();
        final String url = getJavadocIoUrl(artifact);

        offlineLink.setUrl(url);
        offlineLink.setLocation(destDir.getAbsolutePath());
        offlineLinks.add(offlineLink);
      }

      return dependencyStatusSets;
    }
    catch (final IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}