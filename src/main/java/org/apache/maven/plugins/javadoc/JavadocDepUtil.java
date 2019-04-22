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
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

final class JavadocDepUtil {
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
  static <T>void setField(final Class<?> cls, final AbstractMojo mojo, final String name, final Function<T,T> value) {
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

  private JavadocDepUtil() {
  }
}