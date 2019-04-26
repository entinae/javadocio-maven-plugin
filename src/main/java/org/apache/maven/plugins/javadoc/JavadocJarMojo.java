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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.sound.midi.SysexMessage;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.javadoc.options.OfflineLink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

@Mojo(name="jar", requiresDependencyResolution=ResolutionScope.TEST, defaultPhase=LifecyclePhase.GENERATE_SOURCES, threadSafe=true)
@Execute(phase=LifecyclePhase.GENERATE_SOURCES)
public class JavadocJarMojo extends JavadocJar implements SharedMojo {
  private static final ReverseExecutor reverseExecutor = new ReverseExecutor();

  @Component
  private ArchiverManager _archiverManager;

  @Component
  private ArtifactResolver _artifactResolver;

  @Component
  private DependencyResolver _dependencyResolver;

  @Component
  private RepositoryManager _repositoryManager;

  @Component
  private ProjectBuilder _projectBuilder;

  @Component
  private ArtifactHandlerManager _artifactHandlerManager;

  @Parameter(defaultValue="${reactorProjects}", required=true, readonly=true)
  private List<MavenProject> _reactorProjects;

  @Parameter(defaultValue="${settings}", readonly=true, required=true)
  private Settings _settings;

  @Parameter(defaultValue="${detectGeneratedSourcePaths}")
  private boolean detectGeneratedSourcePaths;

  @Override
  public boolean isAggregator() {
    return "pom".equalsIgnoreCase(project.getPackaging());
  }

  @Override
  protected Map<String,Collection<String>> getSourcePaths() throws MavenReportException {
    return filterSourcePaths(super.getSourcePaths(), project);
  }

  @Override
  public List<OfflineLink> collectOfflineLinks() throws MojoExecutionException, MojoFailureException {
    return UnpackDependencies.execute(getLog(), _settings, project, session, _reactorProjects, _archiverManager, _artifactResolver, _dependencyResolver, _repositoryManager, _projectBuilder, _artifactHandlerManager);
  }

  @Override
  public void executeSuperReport(final Locale unusedLocale) throws MavenReportException {
    super.executeReport(unusedLocale);
  }

  @Override
  protected void executeReport(final Locale unusedLocale) throws MavenReportException {
    if (detectGeneratedSourcePaths)
      addGeneratedSourcePaths(project);

    executeReport(project, session, reverseExecutor, unusedLocale);
  }
}