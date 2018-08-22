package de.sormuras.junit.platform.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

class JUnitPlatformConsoleStarter implements IntSupplier {

  private final Configuration configuration;
  private final ModularWorld world;

  JUnitPlatformConsoleStarter(Configuration configuration, ModularWorld world) {
    this.configuration = configuration;
    this.world = world;
  }

  @Override
  public int getAsInt() {
    var log = configuration.getLog();
    var target = Paths.get(configuration.getMavenProject().getBuild().getDirectory());
    var timeout = configuration.getTimeout().toSeconds();
    var reports = configuration.getReportsPath();
    var mainMod = world.getTestModuleReference();
    var testMod = world.getTestModuleReference();
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
    if (!mainMod.isPresent() && !testMod.isPresent()) {
      builder.command().add("--class-path");
      builder.command().add(createModulePathArgument());
      builder.command().add("org.junit.platform.console.ConsoleLauncher");
    }
    if (testMod.isPresent()) {
      builder.command().add("--module-path");
      builder.command().add(createModulePathArgument());
      builder.command().add("--add-modules");
      builder.command().add("ALL-MODULE-PATH,ALL-DEFAULT");
      builder.command().add("--module");
      builder.command().add("org.junit.platform.console");
    }

    // Now append console launcher options
    // See https://junit.org/junit5/docs/snapshot/user-guide/#running-tests-console-launcher-options
    builder.command().add("--disable-ansi-colors");
    builder.command().add("--details");
    builder.command().add("tree");
    if (configuration.isStrict()) {
      builder.command().add("--fail-if-no-tests");
    }
    configuration.getTags().forEach(builder.command()::add);
    configuration
        .getParameters()
        .forEach((key, value) -> builder.command().add(createConfigArgument(key, value)));
    reports.ifPresent(
        path -> {
          builder.command().add("--reports-dir");
          builder.command().add(path.toString());
        });

    if (testMod.isPresent()) {
      builder.command().add("--select-module");
      builder.command().add(testMod.get().descriptor().name());
    } else {
      builder.command().add("--scan-class-path");
    }

    // Start
    log.debug("Starting process...");
    builder.command().forEach(log::debug);
    try {
      var process = builder.start();
      log.debug("Process started: #" + process.pid() + " " + process.info());
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

  private String createModulePathArgument() {
    var log = configuration.getLog();
    var project = configuration.getMavenProject();
    var elements = new ArrayList<String>();
    try {
      for (var element : project.getTestClasspathElements()) {
        var path = Paths.get(element).toAbsolutePath().normalize();
        if (Files.notExists(path)) {
          log.debug("   X " + path + " // doesn't exist");
          continue;
        }
        log.debug("  -> " + path);
        elements.add(path.toString());
      }
    } catch (DependencyResolutionRequiredException e) {
      throw new IllegalStateException("Resolving test class-path elements failed", e);
    }
    return String.join(File.pathSeparator, elements);
  }

  private static Path getCurrentJavaExecutablePath() {
    var path = ProcessHandle.current().info().command().map(Paths::get).orElseThrow();
    return path.normalize().toAbsolutePath();
  }
}
