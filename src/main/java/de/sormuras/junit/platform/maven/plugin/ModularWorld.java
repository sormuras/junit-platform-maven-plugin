package de.sormuras.junit.platform.maven.plugin;

import static java.util.stream.Collectors.joining;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Optional;

public class ModularWorld {

  private final Path mainPath;
  private final Path testPath;

  ModularWorld(Path mainPath, Path testPath) {
    this.mainPath = mainPath;
    this.testPath = testPath;
  }

  public Path getMainPath() {
    return mainPath;
  }

  public Path getTestPath() {
    return testPath;
  }

  public ModularMode getMode() {
    return getMode(mainPath, testPath);
  }

  public Optional<ModuleReference> getMainModuleReference() {
    return Optional.ofNullable(getSingleModuleReferenceOrNull(getMainPath()));
  }

  public Optional<ModuleReference> getTestModuleReference() {
    return Optional.ofNullable(getSingleModuleReferenceOrNull(getTestPath()));
  }

  public static ModularMode getMode(Path main, Path test) {
    var mainModule = getSingleModuleReferenceOrNull(main) == null ? 0 : 2;
    var testModule = getSingleModuleReferenceOrNull(test) == null ? 0 : 1;
    var value = mainModule + testModule;
    switch (value) {
      case 0:
        return ModularMode.MAIN_PLAIN_TEST_PLAIN;
      case 1:
        return ModularMode.MAIN_PLAIN_TEST_MODULE;
      case 2:
        return ModularMode.MAIN_MODULE_TEST_PLAIN;
      case 3:
        return ModularMode.MAIN_MODULE_TEST_MODULE;
      default:
        throw new AssertionError("Expected value in range from 0 to 3, but got: " + value);
    }
  }

  public static ModuleReference getSingleModuleReferenceOrNull(Path path) {
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
