package org.apache.maven.plugins.javadoc;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;

public interface SharedMojo {
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
}