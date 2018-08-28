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

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

class Modules {

  /** Describes the barrier or boundary at which tests may operate. */
  enum Barrier {
    /** Test code may only access exported public types. */
    MODULE,

    /** Test code may access package-level types. */
    PACKAGE
  }

  /**
   * Test-run mode.
   *
   * <p>Defined by the relation of one {@code main} and one {@code test} module.
   *
   * @see <a href="https://stackoverflow.com/a/33627846/1431016">Access Modifier Table</a>
   */
  enum Mode {

    /** No modules at all, fall-back to the class-path. */
    CLASSIC(Barrier.PACKAGE),

    /**
     * Main module absent, test module present.
     *
     * <p>This mode also to be selected when there is no main artifact available, i.e. main is
     * empty.
     */
    MAIN_CLASSIC_TEST_MODULE(Barrier.MODULE),

    /**
     * Main module present, test module absent.
     *
     * <p>White-box testing by patching tests into the main module at test-runtime.
     *
     * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
     */
    MAIN_MODULE_TEST_CLASSIC(Barrier.PACKAGE),

    /**
     * Main module present, test module present: <strong>same</strong> module name.
     *
     * <p>White-box testing by patching main types into the test module at compile-runtime.
     *
     * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
     */
    MAIN_MODULE_TEST_MODULE_SAME_NAME(Barrier.PACKAGE),

    /**
     * Main module present, test module present: <strong>different</strong> module name.
     *
     * <p>Black-box testing the main module, adhering to its exported API.
     */
    MODULAR(Barrier.MODULE);

    private final Barrier barrier;

    Mode(Barrier barrier) {
      this.barrier = barrier;
    }

    @Override
    public String toString() {
      return name() + " [barrier=" + barrier + "]";
    }

    static Mode of(String main, String test) {
      if (main == null) {
        if (test == null) { // trivial case: no modules declared at all
          return CLASSIC;
        }
        return MAIN_CLASSIC_TEST_MODULE; // only test module present
      }
      if (test == null) { // only main module is present
        return MAIN_MODULE_TEST_CLASSIC;
      }
      if (main.equals(test)) { // same module name
        return MAIN_MODULE_TEST_MODULE_SAME_NAME;
      }
      return MODULAR; // true-modular testing, no patching involved
    }
  }

  private final ModuleReference mainModuleReference;
  private final ModuleReference testModuleReference;
  private final Mode mode;

  Modules(Path mainPath, Path testPath) {
    this.mainModuleReference = getSingleModuleReferenceOrNull(mainPath);
    this.testModuleReference = getSingleModuleReferenceOrNull(testPath);
    this.mode =
        Mode.of(getModuleNameOrNull(mainModuleReference), getModuleNameOrNull(testModuleReference));
  }

  Mode getMode() {
    return mode;
  }

  Optional<ModuleReference> getMainModuleReference() {
    return Optional.ofNullable(mainModuleReference);
  }

  Optional<ModuleReference> getTestModuleReference() {
    return Optional.ofNullable(testModuleReference);
  }

  @Override
  public String toString() {
    return String.format("Modules [main=%s, test=%s]", toStringMainModule(), toStringTestModule());
  }

  String toStringMainModule() {
    return toString(mainModuleReference);
  }

  String toStringTestModule() {
    return toString(testModuleReference);
  }

  static ModuleReference getSingleModuleReferenceOrNull(Path path) {
    Set<ModuleReference> all;
    try {
      all = ModuleFinder.of(path).findAll();
    } catch (FindException e) {
      // e.printStackTrace();
      return null;
    }
    var firstOpt = all.stream().findFirst();
    switch (all.size()) {
      case 0:
        return null;
      case 1:
        return firstOpt.get();
      default:
        throw new IllegalArgumentException(
            "expected exactly one module in "
                + path
                + " but found: "
                + all.stream().map(ModuleReference::toString).collect(joining(", ", "<", ">")));
    }
  }

  private static String getModuleNameOrNull(ModuleReference reference) {
    if (reference == null) {
      return null;
    }
    return reference.descriptor().name();
  }

  private static String toString(ModuleReference reference) {
    if (reference == null) {
      return "<empty>";
    }
    var module = reference.descriptor();
    var builder = new StringBuilder();
    if (module.isOpen()) {
      builder.append("open ");
    }
    builder.append("module ").append(module.name());
    builder.append(" {");
    builder
        .append(" requires=")
        .append(
            module
                .requires()
                .stream()
                .map(ModuleDescriptor.Requires::name)
                .collect(joining(", ", "[", "]")));
    builder.append(" packages=").append(module.packages());
    builder.append(" }");
    return builder.toString();
  }
}
