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
 * <p>The test-run mode is defined by the relation of one {@code main} and one {@code test} module
 * name.
 *
 * <ul>
 *   <li>{@code C = CLASSIC}
 *       <p>no modules available
 *   <li>{@code M = MODULAR}
 *       <p>main 'module foo' and test 'module bar' OR main lacks module and test 'module any'
 *   <li>{@code A = MODULAR_PATCHED_TEST_COMPILE}
 *       <p>main 'module foo' and test 'module foo'
 *   <li>{@code B = MODULAR_PATCHED_TEST_RUNTIME}
 *       <p>main 'module foo' and test lacks module
 * </ul>
 *
 * <pre><code>
 *                      main plain    main module   main module
 *                         ---            foo           bar
 * test plain  ---          C              B             B
 * test module foo          M              A             M
 * test module bar          M              M             A
 * </code></pre>
 *
 * @see <a href="https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world">Testing In
 *     The Modular World</a>
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
   * Returns the test mode constant based on two specified module names.
   *
   * @param main name of the main module
   * @param test name of the test module
   * @return test mode constant based on both names
   */
  public static TestMode of(String main, String test) {
    var mainAbsent = main == null || main.isBlank();
    var testAbsent = test == null || test.isBlank();
    if (mainAbsent) {
      if (testAbsent) { // trivial case: no modules declared at all
        return CLASSIC;
      }
      return MODULAR; // only test module is present, no patching involved
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
