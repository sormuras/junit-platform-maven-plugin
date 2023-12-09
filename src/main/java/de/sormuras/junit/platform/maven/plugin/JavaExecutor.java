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
import de.sormuras.junit.platform.isolator.Modules;
import de.sormuras.junit.platform.isolator.TestMode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Forks an external Java process to start the JUnit Platform Console Launcher. */
class JavaExecutor {

  private final JUnitPlatformMojo mojo;
  private final JavaOptions options;
  private final Modules modules;

  JavaExecutor(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.options = mojo.getJavaOptions();
    this.modules = mojo.getProjectModules();
  }

  int evaluate(Configuration configuration) {
    Path target = Paths.get(configuration.basic().getTargetDirectory());
    Path cmdPath = target.resolve("console-launcher.cmd.log");
    Path errorPath = target.resolve("console-launcher.err.log");
    Path outputPath = target.resolve("console-launcher.out.log");

    // Prepare the process builder
    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(mojo.getMavenProject().getBasedir()); // todo: config?
    List<String> cmd = builder.command();

    boolean inheritIO = mojo.getJavaOptions().inheritIO;
    boolean captureIO = !inheritIO;
    if (inheritIO) {
      builder.inheritIO();
    } else {
      builder.redirectError(errorPath.toFile());
      builder.redirectOutput(outputPath.toFile());
      builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
    }

    if (options.additionalEnvironment != null && !options.additionalEnvironment.isEmpty()) {
      builder.environment().putAll(options.additionalEnvironment);
    }

    // "java[.exe]"
    cmd.add(mojo.getJavaExecutable());
    addJavaOptions(cmd, configuration);
    addLauncherOptions(cmd, configuration);

    // Prepare target directory...
    try {
      Files.createDirectories(target);
      Files.write(cmdPath, cmd);
      if (captureIO) {
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
    int exitValue = -1;
    try {
      Process process = builder.start();
      // Java 11 debug("Process started: #%d %s", process.pid(), process.info());
      mojo.debug("Process started: {0}", process);
      long progressTimeout = mojo.getExecutionProgress();
      long globalTimeout = mojo.getTimeout();
      long elapsedTime = 0L;
      boolean completed = false;
      while (!completed && elapsedTime < globalTimeout) {
        if (progressTimeout > globalTimeout - elapsedTime) {
          progressTimeout = globalTimeout - elapsedTime;
        }
        completed = process.waitFor(progressTimeout, TimeUnit.SECONDS);
        mojo.info(
            "Output Log: {0,number,integer} bytes, Error Log: {1,number,integer} bytes",
            Files.exists(outputPath) ? Files.size(outputPath) : 0L,
            Files.exists(errorPath) ? Files.size(errorPath) : 0L);
        elapsedTime += progressTimeout;
      }
      if (!completed) {
        mojo.warn("Global timeout of {0,number,integer} second(s) reached.", globalTimeout);
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
      exitValue = process.exitValue();
      if (captureIO) {
        String encoding = mojo.getCharset();
        if (encoding == null) {
          encoding = System.getProperty("native.encoding"); // Populated on Java 18 and later
        }
        Charset charset = encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
        try (Stream<String> stdoutput = Files.lines(outputPath, charset);
            Stream<String> erroutput = Files.lines(errorPath, charset)) {
          stdoutput.forEach(exitValue == 0 ? mojo::info : mojo::error);
          erroutput.forEach(exitValue == 0 ? mojo::warn : mojo::error);
        } catch (IOException e) {
          mojo.warn("Reading output/error logs failed: {0}", e);
        }
      }
    } catch (CharacterCodingException cce) {
      // Possibly thrown from Files.lines and not caught above
      mojo.warn("Charset error reading output/error logs: {0}", cce);
    } catch (IOException | InterruptedException e) {
      mojo.error("Executing process failed: {0}", e);
      return -1;
    }
    return exitValue;
  }

  // Supply standard options for Java foundation tool
  private void addJavaOptions(List<String> cmd, Configuration configuration) {
    List<String> overrides = options.overrideJavaOptions;
    if (overrides != Collections.EMPTY_LIST) {
      cmd.addAll(overrides);
      return;
    }

    Optional<Object> mainModule = modules.getMainModuleReference();
    Optional<Object> testModule = modules.getTestModuleReference();
    for (String option : options.additionalOptions) {
      if (option == null || option.trim().isEmpty()) continue;
      cmd.add(option);
    }
    if (configuration.basic().isDefaultAssertionStatus()) {
      cmd.add("-enableassertions");
    }
    if (!options.encoding.isEmpty()) {
      cmd.add("-Dfile.encoding=" + options.encoding);
    }
    if (!"false".equalsIgnoreCase(options.debug)) {
      cmd.add(
          "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="
              + (Boolean.parseBoolean(options.debug) /* if bool */ ? "5005" : options.debug));
    }
    if (mainModule.isPresent() || testModule.isPresent()) {
      cmd.add("--module-path");
      cmd.add(createPathArgument(configuration));
      cmd.add("--add-modules");
      cmd.add(createAddModulesArgument());
      if (mainModule.isPresent() && !testModule.isPresent()) {
        new JavaPatcher(mojo, configuration).patch(cmd);
      }
      cmd.add("--module");
      cmd.add("org.junit.platform.console");
    } else {
      cmd.add("-classpath"); // https://github.com/sormuras/junit-platform-maven-plugin/issues/28
      cmd.add(createPathArgument(configuration));
      cmd.add("org.junit.platform.console.ConsoleLauncher");
    }
  }

  // Append console launcher options
  // See https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher-options
  private void addLauncherOptions(List<String> cmd, Configuration configuration) {
    List<String> overrides = options.overrideLauncherOptions;
    if (overrides != Collections.EMPTY_LIST) {
      cmd.addAll(overrides);
      return;
    }

    cmd.add("execute");

    Configuration.Basic basic = configuration.basic();
    Configuration.Discovery dsc = configuration.discovery();

    if (basic.isFailIfNoTests()) {
      cmd.add("--fail-if-no-tests");
    }

    if (mojo.getTweaks().disableAnsi) {
      cmd.add("--disable-ansi-colors");
    }
    cmd.add("--details");
    cmd.add(mojo.getTweaks().details);
    cmd.add("--details-theme");
    cmd.add(mojo.getTweaks().detailsTheme);
    cmd.add("--reports-dir");
    cmd.add(basic.getTargetDirectory());
    dsc.getFilterTags().forEach(tag -> cmd.add(createTagArgument("include", tag)));
    if (mojo.getTest() != null) { // interactive mode first
      if (mojo.getTest().contains("(") || mojo.getTest().contains("#")) {
        cmd.add("--select-method=" + mojo.getTest());
      } else if (Files.exists(
          Paths.get(mojo.getMavenProject().getBuild().getTestOutputDirectory())
              .resolve(mojo.getTest().replace('.', '/') + ".class"))) {
        cmd.add("--select-class=" + mojo.getTest());
      } else {
        cmd.add(
            "--scan-class-path"); // ideally we would use --select-directory but it is buggin in 1.7
        cmd.add("--include-classname=.*\\." + mojo.getTest());
      }
    } else if (dsc.getFilterClassNamePatterns() != null) { // else explicit config
      dsc.getFilterClassNamePatterns().forEach(it -> cmd.add("--include-classname=" + it));
    }
    dsc.getParameters().forEach((key, value) -> cmd.add(createConfigArgument(key, value)));

    Optional<Object> mainModule = modules.getMainModuleReference();
    Optional<Object> testModule = modules.getTestModuleReference();
    if (mojo.getTest() == null) {
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
    if (options.additionalLauncherOptions != null && !options.additionalLauncherOptions.isEmpty()) {
      cmd.addAll(options.additionalLauncherOptions);
    }
  }

  private String createAddModulesArgument() {
    String value = options.addModulesArgument;
    if (value != null && !value.isEmpty()) {
      return value;
    }
    if (modules.getMode() == TestMode.MODULAR_PATCHED_TEST_RUNTIME) {
      return modules.getMainModuleName().orElseThrow(AssertionError::new);
    }
    return modules.getTestModuleName().orElseThrow(AssertionError::new);
  }

  private static String createConfigArgument(String key, String value) {
    return "--config=" + key + "=" + value;
  }

  private static String createTagArgument(String filter, String tag) {
    return "--" + filter + "-tag=" + tag;
  }

  private String createPathArgument(Configuration configuration) {
    final boolean dropMainClasses =
        modules.getMainModuleName().isPresent()
            && modules.getTestModuleName().isPresent()
            && modules
                .getTestModuleName()
                .orElseThrow(IllegalStateException::new)
                .equals(modules.getMainModuleName().orElseThrow(IllegalStateException::new));
    return configuration.basic().getPaths().values().stream()
        .flatMap(Collection::stream)
        .filter(path -> !dropMainClasses || !isMain(configuration, path))
        .collect(Collectors.joining(File.pathSeparator));
  }

  private boolean isMain(final Configuration configuration, final String path) {
    return path.equals(configuration.basic().getTargetMainPath());
  }
}
