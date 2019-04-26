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

import java.util.List;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.repository.RepositoryManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

@Mojo(name="jar", requiresDependencyResolution=ResolutionScope.TEST, defaultPhase=LifecyclePhase.GENERATE_SOURCES, threadSafe=true)
@Execute(phase=LifecyclePhase.GENERATE_SOURCES)
public class JavadocJarMojo extends JavadocJar {
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

  @Parameter(defaultValue="${settings.offline}", required=true, readonly=true)
  protected boolean _offline;

  private Boolean _isAggregator;

  @Override
  protected boolean isAggregator() {
    return _isAggregator == null ? _isAggregator = "pom".equalsIgnoreCase(project.getPackaging()) : _isAggregator;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
//    MojoUtil.invoke(getLog(), _urlOverrrides, _offline, project, session, _reactorProjects, _archiverManager, _artifactResolver, _dependencyResolver, _repositoryManager, _projectBuilder, _artifactHandlerManager);
    if (isAggregator())
      project.setExecutionRoot(true);

    super.execute();
  }
}