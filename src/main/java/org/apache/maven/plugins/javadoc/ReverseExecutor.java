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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.project.MavenProject;

public class ReverseExecutor {
  private final Module rootModule = new Module();

  private class Module {
    private final Map<String,Module> modules = new HashMap<>();
    private final MavenProject project;
    private final Runnable runnable;
    private final String name;
    private Module parent;

    private Module(final MavenProject project, final Runnable runnable) {
      this.project = Objects.requireNonNull(project);
      this.runnable = runnable;
      if (project.hasParent() && project.getParent().getBasedir() != null) {
        try {
          this.name = project.getBasedir().getAbsolutePath().substring(project.getParent().getBasedir().getAbsolutePath().length() + 1);
        }
        catch (final StringIndexOutOfBoundsException e) {
          final StringIndexOutOfBoundsException exception = new StringIndexOutOfBoundsException(e.getMessage() + ": \"" + project.getBasedir().getAbsolutePath() + "\".substring(\"" + project.getParent().getBasedir().getAbsolutePath() + "\".length() + 1");
          exception.initCause(e.getCause());
          throw exception;
        }
      }
      else {
        this.name = project.getBasedir().getName();
      }

      for (final String module : new ArrayList<>(project.getModules()))
        this.modules.put(module, null);
    }

    private Module() {
      this.project = null;
      this.runnable = null;
      this.name = null;
    }

    private void addModule(final Module module) {
      if (project != null && !modules.containsKey(module.name))
        throw new IllegalStateException("Module not found: " + module.name + " in " + modules.keySet());

      if (module.parent != null)
        throw new IllegalStateException("Parent was already set");

      module.parent = this;
      modules.put(module.name, module);
    }

    private Module processModule(final String qualifiedName, final boolean remove) {
      String name = qualifiedName;
      while (true) {
        final Module module = remove ? modules.remove(name) : modules.get(name);
        if (module != null) {
          if (name.equals(qualifiedName)) {
            if (remove) {
              if (module.modules.size() > 0)
                throw new IllegalStateException("Expected to remove empty sub-module");

              module.runnable.run();
              if (parent != null && modules.size() == 0)
                parent.removeModule(this.name);
            }

            return module;
          }

          return module.processModule(qualifiedName.substring(name.length() + 1), remove);
        }

        final int slash = name.lastIndexOf('/');
        if (slash == -1)
          throw new IllegalStateException("Module (qualified '" + qualifiedName + "') not found: " + name + " in " + modules.keySet());

        name = name.substring(0, slash);
      }
    }

    public Module removeModule(final String qualifiedName) {
      return processModule(qualifiedName, true);
    }

    public Module getModule(final String qualifiedName) {
      return processModule(qualifiedName, false);
    }

    @Override
    public String toString() {
      return name + ": " + modules.toString();
    }
  }

  private String rootDir;

  public void submit(final MavenProject project, final Runnable runnable) {
    final Module module = new Module(project, runnable);
    final String parentPath;
    if (project.hasParent() && project.getParent().getBasedir() != null)
      parentPath = project.getParent().getBasedir().getAbsolutePath();
    else
      parentPath = project.getBasedir().getParentFile().getAbsolutePath();

    if (rootDir == null)
      rootDir = parentPath + "/";

    final Module parent;
    if (parentPath.length() <= rootDir.length())
      parent = rootModule;
    else
      parent = rootModule.getModule(parentPath.substring(rootDir.length()));

    parent.addModule(module);
    if (project.getModules().isEmpty())
      parent.removeModule(project.getBasedir().getName());
  }
}