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

import de.sormuras.junit.platform.isolator.Configuration;
import de.sormuras.junit.platform.isolator.Driver;
import de.sormuras.junit.platform.isolator.Modules;
import de.sormuras.junit.platform.isolator.TestMode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Forks an external Java process to start the JUnit Platform Console Launcher. */
class JavaExecutor {

  private final JUnitPlatformMojo mojo;
  private final Modules modules;
  private final Driver driver;

  JavaExecutor(JUnitPlatformMojo mojo, Driver driver) {
    this.mojo = mojo;
    this.modules = mojo.getProjectModules();
    this.driver = driver;
  }

  int evaluate(Configuration configuration) {
    Path target = Paths.get(configuration.basic().getTargetDirectory());
    Path cmdPath = target.resolve("console-launcher.cmd.log");
    Path errorPath = target.resolve("console-launcher.err.log");
    Path outputPath = target.resolve("console-launcher.out.log");

    // Prepare the process builder
    ProcessBuilder builder = new ProcessBuilder();
    List<String> cmd = builder.command();

    boolean inheritIO = mojo.getJavaOptions().inheritIO;
    if (inheritIO) {
      builder.inheritIO();
    } else {
      builder.redirectError(errorPath.toFile());
      builder.redirectOutput(outputPath.toFile());
      builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
    }

    // "java[.exe]"
    cmd.add(mojo.getJavaExecutable());
    addJavaOptions(cmd);
    addLauncherOptions(cmd, configuration);

    // Prepare target directory...
    try {
      Files.createDirectories(target);
      Files.write(cmdPath, cmd);
      if (!inheritIO) {
        if (Files.notExists(errorPath)) {
          Files.createFile(errorPath);
        }
        if (Files.notExists(outputPath)) {
          Files.createFile(outputPath);
        }
      }
    } catch (IOException e) {
      mojo.warn("Preparing target path failed: {0} // {1}", target, e);
    }

    // In dry-run mode, we're done here.
    if (mojo.isDryRun()) {
      mojo.info("Dry-run mode is active -- only printing command line");
      cmd.forEach(mojo::info);
      return 0;
    }

    // Start
    mojo.debug("");
    mojo.debug("Starting process...");
    cmd.forEach(mojo::debug);
    try {
      Process process = builder.start();
      // Java 11 debug("Process started: #%d %s", process.pid(), process.info());
      mojo.debug("Process started: {0}", process);
      if (!process.waitFor(mojo.getTimeout(), TimeUnit.SECONDS)) {
        mojo.warn("Global timeout of " + mojo.getTimeout() + " second(s) reached.");
        process.destroy();
        // give process a second to terminate normally
        for (int i = 10; i > 0 && process.isAlive(); i--) {
          Thread.sleep(123);
        }
        // if the process is still alive, kill it
        if (process.isAlive()) {
          mojo.warn("Killing java process...");
          process.destroyForcibly();
          for (int i = 10; i > 0 && process.isAlive(); i--) {
            Thread.sleep(1234);
          }
        }
        return -2;
      }
      int exitValue = process.exitValue();
      if (!inheritIO) {
        try {
          Files.readAllLines(outputPath)
              .stream()
              .limit(500)
              .forEach(exitValue == 0 ? mojo::info : mojo::error);
          Files.readAllLines(errorPath).forEach(exitValue == 0 ? mojo::warn : mojo::error);
        } catch (IOException e) {
          mojo.warn("Reading output/error logs failed: {0}", e);
        }
      }
      return exitValue;
    } catch (IOException | InterruptedException e) {
      mojo.error("Executing process failed: {0}", e);
      return -1;
    }
  }

  // Supply standard options for Java foundation tool
  private void addJavaOptions(List<String> cmd) {
    List<String> overrides = mojo.getJavaOptions().overrideJavaOptions;
    if (overrides != Collections.EMPTY_LIST) {
      cmd.addAll(overrides);
      return;
    }

    Optional<Object> mainModule = modules.getMainModuleReference();
    Optional<Object> testModule = modules.getTestModuleReference();
    cmd.addAll(mojo.getJavaOptions().additionalOptions);
    cmd.add("-enableassertions");
    cmd.add("-Dfile.encoding=UTF-8");
    if (mainModule.isPresent() || testModule.isPresent()) {
      cmd.add("--module-path");
      cmd.add(createPathArgument());
      cmd.add("--add-modules");
      cmd.add(createAddModulesArgument());
      if (mainModule.isPresent() && !testModule.isPresent()) {
        new JavaPatcher(mojo).patch(cmd);
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
  // See https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher-options
  private void addLauncherOptions(List<String> cmd, Configuration configuration) {
    List<String> overrides = mojo.getJavaOptions().overrideLauncherOptions;
    if (overrides != Collections.EMPTY_LIST) {
      cmd.addAll(overrides);
      return;
    }

    Configuration.Basic basic = configuration.basic();
    Configuration.Discovery dsc = configuration.discovery();

    cmd.add("--disable-ansi-colors");
    cmd.add("--details");
    cmd.add("tree");
    cmd.add("--details-theme");
    cmd.add("ascii");
    cmd.add("--reports-dir");
    cmd.add(basic.getTargetDirectory());
    dsc.getFilterTags().forEach(tag -> cmd.add(createTagArgument("include", tag)));
    dsc.getParameters().forEach((key, value) -> cmd.add(createConfigArgument(key, value)));

    Optional<Object> mainModule = modules.getMainModuleReference();
    Optional<Object> testModule = modules.getTestModuleReference();
    if (testModule.isPresent()) {
      cmd.add("--select-module");
      cmd.add(modules.getTestModuleName().orElseThrow(AssertionError::new));
    } else {
      if (mainModule.isPresent()) {
        cmd.add("--select-module");
        cmd.add(modules.getMainModuleName().orElseThrow(AssertionError::new));
      } else {
        cmd.add("--scan-class-path");
      }
    }
  }

  private String createAddModulesArgument() {
    String value = mojo.getJavaOptions().addModulesArgument;
    if (value != null && !value.isEmpty()) {
      return value;
    }
    if (modules.getMode() == TestMode.MODULAR_PATCHED_TEST_RUNTIME) {
      return modules.getMainModuleName().orElseThrow(AssertionError::new);
    }
    return modules.getTestModuleName().orElseThrow(AssertionError::new);
  }

  private static String createConfigArgument(String key, String value) {
    return "--config=\"" + key + "\"=\"" + value + "\"";
  }

  private static String createTagArgument(String filter, String tag) {
    return "--" + filter + "-tag=\"" + tag + "\"";
  }

  private String createPathArgument() {
    List<String> elements = new ArrayList<>();
    for (Set<Path> paths : driver.paths().values()) {
      paths.forEach(p -> elements.add(p.toString()));
    }
    return String.join(File.pathSeparator, elements);
  }
}
