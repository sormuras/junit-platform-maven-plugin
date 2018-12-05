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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class SeparatingStarter implements IntSupplier {

  private final JUnitPlatformMojo mojo;
  private final Build build;
  private final Log log;
  private final Resolver resolver;

  public SeparatingStarter(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.build = mojo.getMavenProject().getBuild();
    this.log = mojo.getLog();
    this.resolver = new Resolver(mojo);
  }

  @Override
  public int getAsInt() {
    var oldContext = Thread.currentThread().getContextClassLoader();
    try {
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

      Set<Path> execPaths = new LinkedHashSet<>();
      resolver
          .resolve(
              "com.github.sormuras.junit-platform-manager:junit-platform-manager:master-SNAPSHOT")
          .forEach(artifact -> execPaths.add(artifact.getFile().toPath()));

      execPaths.removeAll(mainPaths);
      execPaths.removeAll(testPaths);

      mojo.debugPath("MAIN (scope=compile) paths", mainPaths);
      mojo.debugPath("TEST (scope=test) paths", testPaths);
      mojo.debugPath("JUnit Platform paths", execPaths);

      ClassLoader parent = getClass().getClassLoader();
      ClassLoader mainLoader = new URLClassLoader("main", urls(mainPaths), parent);
      ClassLoader testLoader = new URLClassLoader("test", urls(testPaths), mainLoader);
      ClassLoader execLoader = new URLClassLoader("exec", urls(execPaths), testLoader);

      Thread.currentThread().setContextClassLoader(execLoader);
      return launch();
    } catch (Exception e) {
      throw rethrow(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private int launch() {
    var executor = Executors.newSingleThreadExecutor();
    var timeout = mojo.getTimeout().toMillis();
    try {
      var future = executor.submit(new SeparatingLauncher(mojo));
      return summarize(future.get(timeout, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      log.error("Launcher execution interrupted", e);
      return -1;
    } catch (ExecutionException e) {
      throw rethrow(e.getCause());
    } catch (TimeoutException e) {
      log.error("Global timeout reached: " + timeout + " millis", e);
      return -2;
    } finally {
      executor.shutdownNow();
    }
  }

  private int summarize(TestExecutionSummary summary) {
    long duration = summary.getTimeFinished() - summary.getTimeStarted();
    boolean success = summary.getTestsFailedCount() == 0 && summary.getContainersFailedCount() == 0;
    if (success) {
      long count = summary.getTestsSucceededCount();
      log.info(
          String.format(
              "Successfully executed %d test%s in %d ms",
              count, (count == 1 ? "" : "s"), duration));
    } else {
      StringWriter message = new StringWriter();
      PrintWriter writer = new PrintWriter(message);
      summary.printTo(writer);
      summary.printFailuresTo(writer);
      for (String line : message.toString().split("\\R")) {
        log.error(line);
      }
    }
    return success ? 0 : 1;
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

  private static URL[] urls(Collection<Path> paths) {
    return paths.stream().map(Path::toUri).map(SeparatingStarter::toUrl).toArray(URL[]::new);
  }

  private static URL[] urls(Path... paths) {
    return urls(Arrays.asList(paths));
  }

  private static URL toUrl(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw rethrow(e);
    }
  }
}
