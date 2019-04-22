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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

final class MojoUtil {
  private static String checkSlash(final String url) {
    return url.charAt(url.length() - 1) != '/' ? url + "/" : url;
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

  static String getModelUrl(final File pomFile) {
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

  static void checkPackageList(final File destDir) throws IOException {
    final File packageListFile = new File(destDir, "package-list");
    if (!packageListFile.exists()) {
      final File elementListFile = new File(destDir, "element-list");
      if (elementListFile.exists())
        FileUtils.copyFile(elementListFile, packageListFile);
    }
  }

  static boolean exists(final String url) {
    try {
      new URL(url).openStream().close();
      return true;
    }
    catch (final IOException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T>void setField(final Class<?> cls, final AbstractMojo mojo, final String name, final Function<T,T> value) {
    try {
      final Field field = cls.getDeclaredField(name);
      field.setAccessible(true);
      field.set(mojo, value.apply((T)field.get(mojo)));
    }
    catch (final NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  static void setField(final Class<?> cls, final AbstractMojo mojo, final String name, final Object value) {
    try {
      final Field field = cls.getDeclaredField(name);
      field.setAccessible(true);
      field.set(mojo, value);
    }
    catch (final NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  static void invoke(final AbstractMojo mojo, final List<UrlOverride> urlOverrrides, final boolean offline, final MavenProject project, final MavenSession session, final List<ArtifactRepository> remoteRepositories, final List<MavenProject> reactorProjects, final ArchiverManager archiverManager, final ArtifactResolver artifactResolver, final DependencyResolver dependencyResolver, final RepositoryManager repositoryManager, final ProjectBuilder projectBuilder, final ArtifactHandlerManager artifactHandlerManager) throws MojoExecutionException, MojoFailureException {
    final Map<String,String> dependencyToUrl = new HashMap<>();
    if (urlOverrrides != null)
      for (final UrlOverride urlOverrride : urlOverrrides)
        dependencyToUrl.put(urlOverrride.getDependency(), urlOverrride.getUrl());

    final UnpackDependencies dependencyMojo = new UnpackDependencies(dependencyToUrl, mojo.getLog(), offline, project, session, project.getRemoteArtifactRepositories(), reactorProjects, archiverManager, artifactResolver, dependencyResolver, repositoryManager, projectBuilder, artifactHandlerManager);
    dependencyMojo.execute();

    setField(AbstractJavadocMojo.class, mojo, "detectLinks", false);
    setField(AbstractJavadocMojo.class, mojo, "detectOfflineLinks", false);
    if (dependencyMojo.getOfflineLinks().size() > 0) {
      MojoUtil.<OfflineLink[]>setField(AbstractJavadocMojo.class, mojo, "offlineLinks", (v) -> {
        final List<OfflineLink> offlineLinks;
        if (v != null && v.length > 0) {
          offlineLinks = new ArrayList<>();
          Collections.addAll(offlineLinks, v);
          offlineLinks.addAll(dependencyMojo.getOfflineLinks());
        }
        else {
          offlineLinks = dependencyMojo.getOfflineLinks();
        }

        return offlineLinks.toArray(new OfflineLink[offlineLinks.size()]);
      });
    }
  }

  private MojoUtil() {
  }
}