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
    private final MavenProject project;
    private final Runnable runnable;
    private final String name;
    private final Map<String,Module> modules;
    private Module parent;

    private Module(final MavenProject project, final Runnable runnable) {
      this.project = Objects.requireNonNull(project);
      this.runnable = runnable;
      this.name = project.getBasedir().getName();
      this.modules = new HashMap<>();
      for (final String module : new ArrayList<>(project.getModules()))
        this.modules.put(module, null);
    }

    private Module() {
      this.project = null;
      this.runnable = null;
      this.name = null;
      this.modules = new HashMap<>();
    }

    private void addModule(final Module module) {
      if (this.project != null && !this.modules.containsKey(module.name))
        throw new IllegalStateException("Module not found: " + module.name);

      if (module.parent != null)
        throw new IllegalStateException("Parent was already set");

      module.parent = this;
      this.modules.put(module.name, module);
    }

    public void removeModule(final String qualifiedName) {
      final int slash = qualifiedName.indexOf('/');
      final String name = slash == -1 ? qualifiedName : qualifiedName.substring(0, slash);
      if (!name.equals(qualifiedName)) {
        getModule(name).removeModule(qualifiedName.substring(slash + 1));
        return;
      }

      final Module removed = modules.remove(name);
      if (removed.modules.size() > 0)
        throw new IllegalStateException("Expected to remove empty sub-module");

      removed.runnable.run();
      if (parent != null && modules.size() == 0)
        parent.removeModule(this.name);
    }

    public Module getModule(final String qualifiedName) {
      final int slash = qualifiedName.indexOf('/');
      final String name = slash == -1 ? qualifiedName : qualifiedName.substring(0, slash);
      final Module module = modules.get(name);
      if (module == null)
        throw new IllegalStateException("Module not found: " + name);

      return name.equals(qualifiedName) ? module : module.getModule(qualifiedName.substring(slash + 1));
    }

    @Override
    public String toString() {
      return name + ": " + modules.toString();
    }
  }

  private String rootDir;

  public void submit(final MavenProject project, final Runnable runnable) {
    final Module module = new Module(project, runnable);
    final String parentPath = project.getBasedir().getParentFile().getAbsolutePath();
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