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

import java.lang.reflect.Field;
import java.util.function.Function;

import org.apache.maven.plugin.AbstractMojo;

final class JavadocDepUtil {
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