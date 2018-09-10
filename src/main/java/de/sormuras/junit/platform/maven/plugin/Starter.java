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

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import org.apache.maven.project.MavenProject;

/** Starts an external Java process to launch the JUnit Platform. */
class Starter implements IntSupplier {

  private final JUnitPlatformMojo mojo;
  private final MavenProject project;
  private final Modules modules;

  Starter(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.project = mojo.getMavenProject();
    this.modules = mojo.getProjectModules();
  }

  private void debug(String format, Object... args) {
    mojo.debug(String.format(format, args));
  }

  @Override
  public int getAsInt() {
    var log = mojo.getLog();
    var names = mojo.getFileNames();
    var target = Paths.get(project.getBuild().getDirectory()).resolve("junit-platform");
    var cmdPath = target.resolve(names.getConsoleLauncherCmdLog());
    var errorPath = target.resolve(names.getConsoleLauncherErrLog());
    var outputPath = target.resolve(names.getConsoleLauncherOutLog());

    // Prepare the process builder
    var builder = new ProcessBuilder();
    var cmd = builder.command();
    // builder.directory(program.getParent().toFile());
    builder.redirectError(errorPath.toFile());
    builder.redirectOutput(outputPath.toFile());
    builder.redirectInput(ProcessBuilder.Redirect.INHERIT);

    // "java[.exe]"
    cmd.add(mojo.getJavaExecutable());

    mojo.getOverrideJavaOptions().ifPresentOrElse(cmd::addAll, () -> addJavaOptions(cmd));
    mojo.getOverrideLauncherOptions().ifPresentOrElse(cmd::addAll, () -> addLauncherOptions(cmd));

    // Prepare target directory...
    try {
      Files.createDirectories(target);
      Files.write(cmdPath, cmd);
      if (Files.notExists(errorPath)) {
        Files.createFile(errorPath);
      }
      if (Files.notExists(outputPath)) {
        Files.createFile(outputPath);
      }
    } catch (IOException e) {
      log.warn("Preparing target path failed: " + target, e);
    }

    // In dry-run mode, we're done here.
    if (mojo.isDryRun()) {
      mojo.getLog().info("Dry-run mode is active -- only printing command line");
      cmd.forEach(mojo.getLog()::info);
      return 0;
    }

    // Start
    debug("");
    debug("Starting process...");
    cmd.forEach(mojo::debug);
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

  // Supply standard options for Java
  // https://docs.oracle.com/javase/10/tools/java.htm
  private void addJavaOptions(List<String> cmd) {
    var mainModule = modules.getMainModuleReference();
    var testModule = modules.getTestModuleReference();
    cmd.addAll(mojo.getJavaOptions().getAdditionalOptions());
    cmd.add("-enableassertions");
    cmd.add("-Dfile.encoding=UTF-8");
    if (mainModule.isPresent() || testModule.isPresent()) {
      cmd.add("--module-path");
      cmd.add(createPathArgument());
      cmd.add("--add-modules");
      cmd.add(createAddModulesArgument());
      if (mainModule.isPresent() && !testModule.isPresent()) {
        new Patcher(mojo).patch(cmd);
      }
      cmd.add("--module");
      cmd.add("org.junit.platform.console");
    } else {
      cmd.add("--class-path");
      cmd.add(createPathArgument());
      cmd.add("org.junit.platform.console.ConsoleLauncher");
    }
  }

  // Append console launcher options
  // See https://junit.org/junit5/docs/snapshot/user-guide/#running-tests-console-launcher-options
  private void addLauncherOptions(List<String> cmd) {
    cmd.add("--disable-ansi-colors");
    cmd.add("--details");
    cmd.add("tree");
    mojo.getTags().forEach(tag -> cmd.add(createTagArgument(tag)));
    mojo.getParameters().forEach((key, value) -> cmd.add(createConfigArgument(key, value)));
    mojo.getReportsPath()
        .ifPresent(
            path -> {
              cmd.add("--reports-dir");
              cmd.add(path.toString());
            });

    var mainModule = modules.getMainModuleReference();
    var testModule = modules.getTestModuleReference();
    if (testModule.isPresent()) {
      cmd.add("--select-module");
      cmd.add(testModule.get().descriptor().name());
    } else {
      if (mainModule.isPresent()) {
        cmd.add("--select-module");
        cmd.add(mainModule.get().descriptor().name());
      } else {
        cmd.add("--scan-class-path");
      }
    }
  }

  private String createAddModulesArgument() {
    var value = mojo.getJavaOptions().getAddModules();
    if (value != null) {
      return value;
    }
    // Or play it generic with "ALL-MODULE-PATH,ALL-DEFAULT"?
    switch (modules.getMode()) {
      case MAIN_MODULE_TEST_CLASSIC:
        return modules.getMainModuleReference().orElseThrow().descriptor().name();
      default:
        return modules.getTestModuleReference().orElseThrow().descriptor().name();
    }
  }

  private static String createConfigArgument(String key, String value) {
    return "--config=\"" + key + "\"=\"" + value + "\"";
  }

  private static String createTagArgument(String tag) {
    return "--include-tag=\"" + tag + "\"";
  }

  private String createPathArgument() {
    var delimiter = File.pathSeparator;
    return mojo.getProjectPaths().stream().map(Object::toString).collect(joining(delimiter));
  }
}
