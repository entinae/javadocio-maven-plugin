/* Copyright (c) 2019 ENTINAE
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

import java.util.Objects;

import org.apache.maven.plugin.logging.Log;

class FilterLog implements Log {
  private final Log log;

  FilterLog(final Log log) {
    this.log = Objects.requireNonNull(log);
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public void debug(final CharSequence content) {
    log.debug(content);
  }

  @Override
  public void debug(final CharSequence content, final Throwable error) {
    log.debug(content, error);
  }

  @Override
  public void debug(final Throwable error) {
    log.debug(error);
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  @Override
  public void info(final CharSequence content) {
    log.info(content);
  }

  @Override
  public void info(final CharSequence content, final Throwable error) {
    log.info(content, error);
  }

  @Override
  public void info(final Throwable error) {
    log.info(error);
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public void warn(final CharSequence content) {
    log.warn(content);
  }

  @Override
  public void warn(final CharSequence content, final Throwable error) {
    log.warn(content, error);
  }

  @Override
  public void warn(final Throwable error) {
    log.warn(error);
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }

  @Override
  public void error(final CharSequence content) {
    log.error(content);
  }

  @Override
  public void error(final CharSequence content, final Throwable error) {
    log.error(content, error);
  }

  @Override
  public void error(final Throwable error) {
    log.error(error);
  }
}