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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

final class MojoUtil {
  private static final int BUFFER_SIZE = 4096;
  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 3000;

  /**
   * Downloads a file from the specified {@code url} to the provided
   * {@code file}. If the provided {@code file} exists, its lastModified
   * timestamp is used to specify the {@code If-Modified-Since} header in the
   * GET request. Content is not downloaded if the file at the specified
   * {@code url} is not modified.
   *
   * @param url The {@link URL} from which to download.
   * @param file The destination {@link File}.
   * @return The HTTP response code.
   * @throws IOException If an I/O error has occurred.
   */
  static int downloadFile(final String url, final File file) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT);
    connection.setReadTimeout(READ_TIMEOUT);
    try {
      connection.setIfModifiedSince(file.lastModified());
      final int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED && responseCode == HttpURLConnection.HTTP_OK) {
        try (
          final InputStream in = connection.getInputStream();
          final FileOutputStream out = new FileOutputStream(file);
        ) {
          final byte[] buffer = new byte[BUFFER_SIZE];
          for (int read; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)); // [ST]
        }
      }

      return responseCode;
    }
    finally {
      connection.disconnect();
    }
  }

  static Model getModelArtifact(final File pomFile)  {
    try {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final Model model = reader.read(new FileReader(pomFile));
      model.setPomFile(pomFile);
      return model;
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

  static String getModelUrl(final File pomFile) {
    final Model model = getModelArtifact(pomFile);
    final String url = cleanUrl(model.getUrl());
    if (url != null)
      return url;

    final String id = getId(model);
    final String artifactDir = pomFile.getParent();
    final String localRepoPath = artifactDir.substring(0, artifactDir.length() - id.length());
    final String parentUrl = getParentPath(localRepoPath, model.getParent());
    return parentUrl + model.getArtifactId() + "/";
  }

  static void checkPackageList(final String destDir) throws IOException {
    final File packageListFile = new File(destDir, "package-list");
    if (!packageListFile.exists()) {
      final File elementListFile = new File(destDir, "element-list");
      if (elementListFile.exists())
        FileUtils.copyFile(elementListFile, packageListFile);
    }
  }

  static boolean exists(final String url) {
    try {
      final HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.connect();
      connection.disconnect();
      return true;
    }
    catch (final IOException e) {
      return false;
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

  private static String cleanUrl(String url) {
    if (url == null)
      return null;

    url = url.trim();
    while (url.endsWith("/"))
      url = url.substring(0, url.lastIndexOf('/'));

    return url + "/";
  }

  static String getJavadocLink(final MavenProject project) {
    return project.getUrl() == null ? null : cleanUrl(project.getUrl()) + "/apidocs";
  }

  private MojoUtil() {
  }
}