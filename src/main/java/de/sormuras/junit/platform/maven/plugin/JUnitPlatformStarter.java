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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

class JUnitPlatformStarter implements IntSupplier {

  private final JUnitPlatformMojo mojo;
  private final Modules modules;

  JUnitPlatformStarter(JUnitPlatformMojo mojo, Modules modules) {
    this.mojo = mojo;
    this.modules = modules;
  }

  private void debug(String format, Object... args) {
    mojo.debug(String.format(format, args));
  }

  @Override
  public int getAsInt() {
    var log = mojo.getLog();
    var build = mojo.getMavenProject().getBuild();
    var target = Paths.get(build.getDirectory());
    var testClasses = build.getTestOutputDirectory();
    var reports = mojo.getReportsPath();
    var mainModule = modules.getMainModuleReference();
    var testModule = modules.getTestModuleReference();
    var errorPath = target.resolve("junit-platform-console-launcher.err.txt");
    var outputPath = target.resolve("junit-platform-console-launcher.out.txt");

    // Prepare the process builder
    var builder = new ProcessBuilder();
    // builder.directory(program.getParent().toFile());
    builder.redirectError(errorPath.toFile());
    builder.redirectOutput(outputPath.toFile());
    builder.redirectInput(ProcessBuilder.Redirect.INHERIT);

    // "java[.exe]"
    builder.command().add(getCurrentJavaExecutablePath().toString());

    // Supply standard options for Java
    // https://docs.oracle.com/javase/10/tools/java.htm
    if (testModule.isPresent()) {
      builder.command().add("--module-path");
      builder.command().add(createPathArgument());
      builder.command().add("--add-modules");
      builder.command().add("ALL-MODULE-PATH,ALL-DEFAULT");
      builder.command().add("--module");
      builder.command().add("org.junit.platform.console");
    } else {
      if (mainModule.isPresent()) {
        builder.command().add("--module-path");
        builder.command().add(createPathArgument());
        builder.command().add("--add-modules");
        builder.command().add("ALL-MODULE-PATH,ALL-DEFAULT");
        var name = mainModule.get().descriptor().name();
        builder.command().add("--patch-module");
        builder.command().add(name + "=" + testClasses);
        builder.command().add("--add-reads");
        builder.command().add(name + "=org.junit.jupiter.api");
        // all packages, due to
        // http://mail.openjdk.java.net/pipermail/jigsaw-dev/2017-January/010749.html
        var packages = mainModule.get().descriptor().packages();
        packages.forEach(
            pkg -> {
              builder.command().add("--add-opens");
              builder.command().add(name + "/" + pkg + "=org.junit.platform.commons");
            });
        builder.command().add("--module");
        builder.command().add("org.junit.platform.console");
      } else {
        builder.command().add("--class-path");
        builder.command().add(createPathArgument());
        builder.command().add("org.junit.platform.console.ConsoleLauncher");
      }
    }

    // Now append console launcher options
    // See https://junit.org/junit5/docs/snapshot/user-guide/#running-tests-console-launcher-options
    builder.command().add("--disable-ansi-colors");
    builder.command().add("--details");
    builder.command().add("tree");
    if (mojo.isStrict()) {
      builder.command().add("--fail-if-no-tests");
    }
    mojo.getTags().forEach(builder.command()::add);
    mojo.getParameters()
        .forEach((key, value) -> builder.command().add(createConfigArgument(key, value)));
    reports.ifPresent(
        path -> {
          builder.command().add("--reports-dir");
          builder.command().add(path.toString());
        });

    if (testModule.isPresent()) {
      builder.command().add("--select-module");
      builder.command().add(testModule.get().descriptor().name());
    } else {
      if (mainModule.isPresent()) {
        builder.command().add("--select-module");
        builder.command().add(mainModule.get().descriptor().name());
      } else {
        builder.command().add("--scan-class-path");
      }
    }

    // Start
    debug("");
    debug("Starting process...");
    builder.command().forEach(mojo::debug);
    try {
      var timeout = mojo.getTimeout().toSeconds();
      var process = builder.start();
      debug("Process started: #%d %s", process.pid(), process.info());
      var ok = process.waitFor(timeout, TimeUnit.SECONDS);
      if (!ok) {
        var s = timeout == 1 ? "" : "s";
        log.error("Global timeout of " + timeout + " second" + s + " reached.");
        log.error("Killing process #" + process.pid());
        process.destroy();
        return -2;
      }
      var exitValue = process.exitValue();
      Files.readAllLines(outputPath).forEach(exitValue == 0 ? log::info : log::error);
      Files.readAllLines(errorPath).forEach(exitValue == 0 ? log::warn : log::error);
      return exitValue;
    } catch (IOException | InterruptedException e) {
      log.error("Executing process failed", e);
      return -1;
    }
  }

  private String createConfigArgument(String key, String value) {
    return "--config=\"" + key + "\"=\"" + value + "\"";
  }

  private String createPathArgument() {
    var project = mojo.getMavenProject();
    var elements = new ArrayList<String>();
    try {
      for (var element : project.getTestClasspathElements()) {
        var path = Paths.get(element).toAbsolutePath().normalize();
        if (Files.notExists(path)) {
          debug("  X %s // doesn't exist", path);
          continue;
        }
        debug(" -> %s", path);
        elements.add(path.toString());
      }
      // junit-platform-console
      resolve(elements, "org.junit.platform", "junit-platform-console", "1.3.0-RC1");
      // junit-jupiter-engine, iff junit-jupiter-api is present
      if (project.getArtifactMap().containsKey("org.junit.jupiter:junit-jupiter-api")) {
        resolve(elements, "org.junit.jupiter", "junit-jupiter-engine", "5.3.0-RC1");
      }
      // junit-vintage-engine, iff junit:junit is present
      if (project.getArtifactMap().containsKey("junit:junit")) {
        resolve(elements, "org.junit.vintage", "junit-vintage-engine", "5.3.0-RC1");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Resolving test class-path elements failed", e);
    }
    return String.join(File.pathSeparator, elements);
  }

  private void resolve(List<String> elements, String group, String artifact, String version)
      throws Exception {
    var map = mojo.getMavenProject().getArtifactMap();
    var ga = group + ':' + artifact;
    if (map.containsKey(ga)) {
      debug("Skip resolving '%s', because it is already mapped.", ga);
      return;
    }
    var gav = ga + ":" + version;
    debug("");
    debug("Resolving '%s' and its transitive dependencies...", gav);
    for (var resolved : mojo.resolve(gav)) {
      var key = resolved.getGroupId() + ':' + resolved.getArtifactId();
      if (map.containsKey(key)) {
        debug("  X %s // mapped by project", resolved);
        continue;
      }
      var path = resolved.getFile().toPath().toAbsolutePath().normalize();
      var element = path.toString();
      if (elements.contains(element)) {
        debug("  X %s // already added", resolved);
        continue;
      }
      debug(" -> %s", element);
      elements.add(element);
    }
  }

  private static Path getCurrentJavaExecutablePath() {
    var path = ProcessHandle.current().info().command().map(Paths::get).orElseThrow();
    return path.normalize().toAbsolutePath();
  }
}
