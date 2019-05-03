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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;

public interface DefaultMojo {
  static OfflineLink[] merge(final OfflineLink[] a, final OfflineLink[] b) {
    final Map<String,OfflineLink> urlToOfflineLink = new LinkedHashMap<>();
    if (a != null)
      for (final OfflineLink link : a)
        urlToOfflineLink.putIfAbsent(link.getUrl(), link);

    if (b != null)
      for (final OfflineLink link : b)
        urlToOfflineLink.putIfAbsent(link.getUrl(), link);

    return urlToOfflineLink.values().toArray(new OfflineLink[urlToOfflineLink.size()]);
  }

  default void setOfflineLinks(final OfflineLink[] offlineLinks) {
    try {
      final Field field = AbstractJavadocMojo.class.getDeclaredField("offlineLinks");
      field.setAccessible(true);
      field.set(this, merge(getOfflineLinks(), offlineLinks));
    }
    catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  default OfflineLink[] getOfflineLinks() {
    try {
      final Field field = AbstractJavadocMojo.class.getDeclaredField("offlineLinks");
      field.setAccessible(true);
      return (OfflineLink[])field.get(this);
    }
    catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  default void setSourcepath(final String sourcepaths) {
    try {
      final Field field = AbstractJavadocMojo.class.getDeclaredField("sourcepath");
      field.setAccessible(true);
      field.set(this, sourcepaths);
    }
    catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  default String getSourcepath() {
    try {
      final Field field = AbstractJavadocMojo.class.getDeclaredField("sourcepath");
      field.setAccessible(true);
      return (String)field.get(this);
    }
    catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  default Map<String,Collection<String>> filterSourcePaths(final Map<String,Collection<String>> sourcePaths, final MavenProject project) {
    final String baseDir = project.getBasedir().getAbsolutePath();
    final Iterator<Map.Entry<String,Collection<String>>> iterator = sourcePaths.entrySet().iterator();
    while (iterator.hasNext()) {
      final Map.Entry<String,Collection<String>> entry = iterator.next();
      final Collection<String> paths = entry.getValue();
      final Iterator<String> pathsIterator = paths.iterator();
      while (pathsIterator.hasNext()) {
        final String path = pathsIterator.next();
        if (!path.startsWith(baseDir))
          pathsIterator.remove();
      }

      if (paths.isEmpty())
        iterator.remove();
    }

    return sourcePaths;
  }

  default void addGeneratedSourcePaths(final MavenProject project) throws MavenReportException {
    try {
      final File generatedSources = new File(project.getBuild().getDirectory(), "generated-sources");
      if (!generatedSources.exists())
        return;

      final List<String> paths = new ArrayList<>();
      Files.walk(generatedSources.toPath()).filter(Files::isRegularFile).forEach((o) -> {
        final File file = o.toFile();
        if (!file.getName().endsWith(".java"))
          return;

        final String filePath = file.getParentFile().getAbsolutePath();
        for (final String path : paths)
          if (filePath.startsWith(path))
            return;

        boolean inBlockQuote = false;
        String packageName = null;
        try (final Scanner scanner = new Scanner(file)) {
          scanner.useDelimiter("\r|\n");
          while (scanner.hasNext()) {
            final String line = scanner.next().trim();
            if (inBlockQuote) {
              // Matches a line that has a closing block comment "*/" sequence
              if (line.matches("^(([^*]|(\\*[^/]))*\\*+/([^/]|(/[^*])|(/$))*)*$"))
                inBlockQuote = false;
              else
                continue;
            }

            if (line.length() == 0 || line.startsWith("//")) {
              continue;
            }

            // Matches a line that has an opening block comment "/*" sequence
            if (line.matches("^(([^/]|(/[^*]))*/+\\*([^*]|(\\*[^/])|(\\*$))*)*$")) {
              inBlockQuote = true;
              continue;
            }

            if (line.startsWith("package ")) {
              packageName = line.substring(8, line.indexOf(';'));
              break;
            }

            if (line.contains("class ") || line.contains("interface ") || line.contains("@interface ") || line.contains("enum ")) {
              break;
            }
          }
        }
        catch (final FileNotFoundException e) {
          throw new IllegalStateException(e);
        }

        if (packageName != null)
          paths.add(filePath.substring(0, filePath.length() - packageName.length() - 1));
        else
          getLog().warn("Could not determine package name of: " + file.getAbsolutePath());
      });

      if (paths.size() == 0)
        return;

      final StringBuilder builder = new StringBuilder();
      for (final String path : paths)
        builder.append(':').append(path);

      String sourcepaths = getSourcepath();
      if (sourcepaths == null || sourcepaths.length() == 0)
        sourcepaths = project.getBuild().getSourceDirectory();

      setSourcepath(sourcepaths + builder);
    }
    catch (final IOException e) {
      throw new MavenReportException(e.getMessage(), e);
    }
  }

  default void executeReport(final MavenProject project, final ReverseExecutor reverseExecutor, final Locale unusedLocale) {
    getLog().debug("Submitting " + project.getName() + " " + project.getVersion());
    reverseExecutor.submit(project, () -> {
      getLog().info("Running " + project.getName() + " " + project.getVersion());
      try {
        final List<OfflineLink> offlineLinks = collectOfflineLinks();
        setOfflineLinks(offlineLinks.toArray(new OfflineLink[offlineLinks.size()]));
        if (isAggregator())
          project.setExecutionRoot(true);

        executeSuperReport(unusedLocale);
      }
      catch (final MavenReportException | MojoExecutionException | MojoFailureException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  Log getLog();
  boolean isAggregator();
  void executeSuperReport(Locale unusedLocale) throws MavenReportException;
  List<OfflineLink> collectOfflineLinks() throws MojoExecutionException, MojoFailureException;
}