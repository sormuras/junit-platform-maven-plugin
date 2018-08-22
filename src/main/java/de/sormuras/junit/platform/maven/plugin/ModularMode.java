package de.sormuras.junit.platform.maven.plugin;

import static java.util.stream.Collectors.joining;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;

public enum ModularMode {

  /** No modules at all -- legacy class-path usage. */
  MAIN_PLAIN_TEST_PLAIN,

  /** Test contains a module descriptor -- */
  MAIN_PLAIN_TEST_MODULE,

  /** Main contains a module descriptors -- needs runtime patching. */
  MAIN_MODULE_TEST_PLAIN,

  /** Main and test contain module descriptors. */
  MAIN_MODULE_TEST_MODULE;

  public static ModularMode of(Path main, Path test) {
    var mainModule = getSingleModuleReferenceOrNull(main) == null ? 0 : 2;
    var testModule = getSingleModuleReferenceOrNull(test) == null ? 0 : 1;
    var value = mainModule + testModule;
    switch (value) {
      case 0:
        return MAIN_PLAIN_TEST_PLAIN;
      case 1:
        return MAIN_PLAIN_TEST_MODULE;
      case 2:
        return MAIN_MODULE_TEST_PLAIN;
      case 3:
        return MAIN_MODULE_TEST_MODULE;
      default:
        throw new AssertionError("Expected value in range from 0 to 3, but got: " + value);
    }
  }

  private static ModuleReference getSingleModuleReferenceOrNull(Path path) {
    var all = ModuleFinder.of(path).findAll();
    var firstOpt = all.stream().findFirst();
    switch (all.size()) {
      case 0:
        return null;
      case 1:
        return firstOpt.get();
      default:
        throw new IllegalArgumentException(
            "expected exact one module in "
                + path
                + " but found: "
                + all.stream().map(ModuleReference::toString).collect(joining(", ", "<", ">")));
    }
  }
}
