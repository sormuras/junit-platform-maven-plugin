package de.sormuras.junit.platform.maven.plugin;

import static java.util.Collections.emptyList;

import java.util.List;

/** Tweak options to fine-tune the test execution. */
@SuppressWarnings("WeakerAccess")
public class TweakOptions {

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
