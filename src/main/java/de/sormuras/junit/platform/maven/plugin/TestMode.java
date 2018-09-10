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

/**
 * Test-run mode.
 *
 * <p>Defined by the relation of one {@code main} and one {@code test} module name.
 *
 * <pre><code>
 *                          main plain    main module   main module
 *                             ---            foo           bar
 *
 *     test plain  ---          0              3             3
 *
 *     test module foo          1              2             1
 *
 *     test module bar          1              1             2
 *
 *     0 = CLASSIC                      // no modules available
 *     1 = MODULAR                      // main lacks module, test 'module any'
 *     1 = MODULAR                      // main 'module foo', test 'module bar'
 *     2 = MODULAR_PATCHED_TEST_COMPILE // main 'module foo', test 'module foo'
 *     3 = MODULAR_PATCHED_TEST_RUNTIME // main 'module foo', test lacks module
 * </code></pre>
 *
 * @see <a href="https://stackoverflow.com/a/33627846/1431016">Access Modifier Table</a>
 */
public enum TestMode {

  /**
   * No modules at all, fall-back to the class-path.
   *
   * <p>Whether the main classes are treated as black or white box is undefined.
   */
  CLASSIC,

  /**
   * Main module present, test module present: <strong>different</strong> module name.
   *
   * <p>Treating the main module as a black box, adhering to its exported API.
   */
  MODULAR,

  /**
   * Main module present, test module present: <strong>same</strong> module name.
   *
   * <p>White-box testing by patching main types into the test module at test compile-time.
   *
   * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
   */
  MODULAR_PATCHED_TEST_COMPILE,

  /**
   * Main module present, test module absent.
   *
   * <p>White-box testing by patching tests into the main module at test-runtime.
   *
   * <p>This mode intentionally by-passes the modular barrier to allow access to internal types.
   */
  MODULAR_PATCHED_TEST_RUNTIME;

  /**
   * Get test mode based on two module names.
   *
   * @param main name of the main module
   * @param test name of the test module
   * @return test mode constant based on both names
   */
  public static TestMode of(String main, String test) {
    var mainAbsent = main == null || main.trim().isEmpty(); // 12: main.isBlank();
    var testAbsent = test == null || test.trim().isEmpty(); // 12: test.isBlank();
    if (mainAbsent) {
      if (testAbsent) { // trivial case: no modules declared at all
        return CLASSIC;
      }
      return MODULAR; // only test module is present
    }
    if (testAbsent) { // only main module is present
      return MODULAR_PATCHED_TEST_RUNTIME;
    }
    if (main.equals(test)) { // same module name
      return MODULAR_PATCHED_TEST_COMPILE;
    }
    return MODULAR; // bi-modular testing, no patching involved
  }
}
