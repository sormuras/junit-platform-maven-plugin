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

package de.sormuras.junit.platform.maven.plugin.testing;

/**
 * Test-run mode.
 *
 * <p>Defined by the relation of one {@code main} and one {@code test} module name.
 *
 * <pre><code>
 *                          main plain    main module   main module
 *                             ---            foo           bar
 *
 *     test plain  ---          0              2             2
 *
 *     test module foo          1              3             4
 *
 *     test module bar          1              4             3
 *
 *     0 = CLASSIC
 *     1 = MAIN_CLASSIC_TEST_MODULE           (black-box test) // CLASSIC_MODULAR_TEST
 *     2 = MAIN_MODULE_TEST_CLASSIC           (white-box test) // MODULAR_CLASSIC_TEST
 *     3 = MAIN_MODULE_TEST_MODULE_SAME_NAME  (white-box test) // MODULAR_PATCHED_TEST
 *     4 = MODULAR                            (black-box test) // MODULAR_BLACKBOX_TEST
 * </code></pre>
 *
 * @see <a href="https://stackoverflow.com/a/33627846/1431016">Access Modifier Table</a>
 */
public enum TestMode {

  /** No modules at all, fall-back to the class-path. */
  CLASSIC(TestBarrier.PACKAGE),

  /**
   * Main module absent, test module present.
   *
   * <p>This mode is also to be selected when there is no main artifact available, i.e. main is
   * empty.
   *
   * <p>Note: A classic non-empty main source set is not supported by `maven-compiler-plugin` at the
   * moment.
   *
   * <pre><code>
   * Caused by: java.lang.UnsupportedOperationException:
   *   Can't compile test sources when main sources are missing a module descriptor at
   *   org.apache.maven.plugin.compiler.TestCompilerMojo.preparePaths (TestCompilerMojo.java:374)
   *   ...
   * </code></pre>
   */
  MAIN_CLASSIC_TEST_MODULE(TestBarrier.MODULE),

  /**
   * Main module present, test module absent.
   *
   * <p>White-box testing by patching tests into the main module at test-runtime.
   *
   * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
   */
  MAIN_MODULE_TEST_CLASSIC(TestBarrier.PACKAGE),

  /**
   * Main module present, test module present: <strong>same</strong> module name.
   *
   * <p>White-box testing by patching main types into the test module at compile-runtime.
   *
   * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
   */
  MAIN_MODULE_TEST_MODULE_SAME_NAME(TestBarrier.PACKAGE),

  /**
   * Main module present, test module present: <strong>different</strong> module name.
   *
   * <p>Black-box testing the main module, adhering to its exported API.
   */
  MODULAR(TestBarrier.MODULE);

  final TestBarrier barrier;

  TestMode(TestBarrier barrier) {
    this.barrier = barrier;
  }

  /**
   * Get test mode based on two module names.
   *
   * @param main name of the main module
   * @param test name of the test module
   * @return test mode constant based on both names
   */
  public static TestMode of(String main, String test) {
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

  @Override
  public String toString() {
    return name() + " [barrier=" + barrier + "]";
  }
}
