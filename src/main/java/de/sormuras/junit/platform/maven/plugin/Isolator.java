/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.junit.platform.maven.plugin;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.DependencyResolutionException;

class Isolator {

  private final JUnitPlatformMojo mojo;
  private final Resolver resolver;

  Isolator(JUnitPlatformMojo mojo, Resolver resolver) {
    this.mojo = mojo;
    this.resolver = resolver;
  }

  ClassLoader createClassLoader()
      throws DependencyResolutionRequiredException, DependencyResolutionException {
    Set<Path> mainPaths =
        mojo.getMavenProject()
            .getCompileClasspathElements()
            .stream()
            .map(Paths::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Path> testPaths =
        mojo.getMavenProject()
            .getTestClasspathElements()
            .stream()
            .map(Paths::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    testPaths.removeAll(mainPaths);

    Set<Path> platformPaths = new LinkedHashSet<>();
    List<Artifact> managers =
        resolver.resolve(
            "com.github.sormuras.junit-platform-manager:junit-platform-manager:master-SNAPSHOT");
    platformPaths.add(managers.get(0).getFile().toPath());

    platformPaths.removeAll(mainPaths);
    platformPaths.removeAll(testPaths);

    mojo.debug("Main (scope=compile) Paths", mainPaths);
    mojo.debug("Test (scope=test) Paths", testPaths);
    mojo.debug("JUnit Platform Paths", platformPaths);

    ClassLoader parent = getClass().getClassLoader();
    ClassLoader mainLoader = new URLClassLoader(urls(mainPaths), parent);
    ClassLoader testLoader = new URLClassLoader(urls(testPaths), mainLoader);

    return new URLClassLoader(urls(platformPaths), testLoader);
  }

  private static URL[] urls(Collection<Path> paths) {
    return paths.stream().map(Path::toUri).map(Isolator::toUrl).toArray(URL[]::new);
  }

  private static URL toUrl(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw rethrow(e);
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
