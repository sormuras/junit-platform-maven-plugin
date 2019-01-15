package de.sormuras.junit.platform.maven.plugin;

import static java.util.Collections.emptyList;

import java.util.List;

/** Tweak options to fine-tune test execution. */
@SuppressWarnings("WeakerAccess")
public class Tweaks {

  /** Fail test run if no tests are found. */
  boolean failIfNoTests = true;

  /** Enable execution of Java language's {@code assert} statements. */
  boolean defaultAssertionStatus = true;

  /** Use platform or thread context classloader. */
  boolean platformClassLoader = true;

  /** Move any test engine implementations to the launcher classloader. */
  boolean moveTestEnginesToLauncherClassLoader = true;

  /** Fail if worker is not loaded in isolation. */
  boolean workerIsolationRequired = true;

  /** A missing test output directory and no explicit selector configured: skip execution. */
  boolean skipOnMissingTestOutputDirectory = true;

  /** List of additional raw (local) test path elements. */
  List<String> additionalTestPathElements = emptyList();

  /** List of additional raw (local) launcher path elements. */
  List<String> additionalLauncherPathElements = emptyList();

  /** List of {@code group:artifact} dependencies to exclude from all path sets. */
  List<String> dependencyExcludes = emptyList();

  /** List of {@code group:artifact:version} dependencies to include in test path set. */
  List<String> additionalTestDependencies = emptyList();

  /** List of {@code group:artifact:version} dependencies to include in launcher path set. */
  List<String> additionalLauncherDependencies = emptyList();
}
