/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.sormuras.junit.platform.maven.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.IntSupplier;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMavenPluginMojo extends AbstractMojo implements Configuration {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "100", readonly = true, required = true)
  private long timeout;

  @Override
  public MavenProject getMavenProject() {
    return project;
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofSeconds(timeout);
  }

  public void execute() throws MojoExecutionException {
    ClassLoader loader = createClassLoader();

    Class<?> taskClass = load(loader, Task.class);
    Class<?>[] taskTypes = new Class<?>[] {ClassLoader.class, Configuration.class};
    IntSupplier task = (IntSupplier) create(taskClass, taskTypes, loader, this);

    int result = task.getAsInt();
    if (result != 0) {
      throw new MojoExecutionException("RED ALERT!");
    }
  }

  private ClassLoader createClassLoader() throws MojoExecutionException {
    ClassLoader parent = getClass().getClassLoader();
    URL[] urls;
    try {
      List<String> elements = project.getTestClasspathElements();
      urls = new URL[project.getTestClasspathElements().size()];
      for (int i = 0; i < elements.size(); i++) {
        urls[i] = Paths.get(elements.get(i)).toAbsolutePath().normalize().toUri().toURL();
      }
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Resolving test class-path elements failed", e);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException("Malformed URL caught: ", e);
    }
    return URLClassLoader.newInstance(urls, parent);
  }

  private static Class<?> load(ClassLoader loader, Class<?> type) {
    String name = type.getName();
    try {
      return loader.loadClass(name);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  private static Object create(Class<?> type, Class<?>[] parameterTypes, Object... args) {
    try {
      return type.getConstructor(parameterTypes).newInstance(args);
    } catch (IllegalAccessException | NoSuchMethodException | InstantiationException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }

  private static UndeclaredThrowableException rethrow(Throwable cause) {
    if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    return new UndeclaredThrowableException(cause);
  }
}
