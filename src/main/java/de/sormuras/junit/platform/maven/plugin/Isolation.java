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

/** Isolation level. */
public enum Isolation {

  /**
   * Total isolation.
   *
   * <pre><code>
   * MAIN
   *   - target/classes
   *   - main dependencies...
   * TEST
   *   - target/test-classes
   *   - test dependencies...
   * JUNIT PLATFORM
   *   - junit-platform-launcher
   *   - junit-jupiter-engine
   *   - junit-vintage-engine
   *   - more runtime-only test engines...
   * ISOLATOR
   *   - junit-platform-isolator
   *   - junit-platform-isolator-worker
   * </code></pre>
   */
  ABSOLUTE,

  /**
   * Almost total isolation - main and test classes are put into the same layer.
   *
   * <pre><code>
   * MAIN
   *   - main dependencies...
   * TEST
   *   - target/classes
   *   - target/test-classes
   *   - test dependencies...
   * JUNIT PLATFORM
   *   - junit-platform-launcher
   *   - junit-jupiter-engine
   *   - junit-vintage-engine
   *   - more runtime-only test engines...
   * ISOLATOR
   *   - junit-platform-isolator
   *   - junit-platform-isolator-worker
   * </code></pre>
   */
  ALMOST,

  /**
   * Merge main and test layers.
   *
   * <pre><code>
   * MERGED (TEST + MAIN)
   *   - target/test-classes
   *   - test dependencies...
   *   - target/classes
   *   - main dependencies...
   * JUNIT PLATFORM
   *   - junit-platform-launcher
   *   - junit-jupiter-engine
   *   - junit-vintage-engine
   *   - more runtime-only test engines...
   * ISOLATOR
   *   - junit-platform-isolator
   *   - junit-platform-isolator-worker
   * </code></pre>
   */
  MERGED,

  /**
   * No isolation, all dependencies are put into a single layer.
   *
   * <pre><code>
   * ALL
   *   - target/classes
   *   - main dependencies...
   *   - target/test-classes
   *   - test dependencies...
   *   - junit-platform-launcher
   *   - junit-jupiter-engine
   *   - junit-vintage-engine
   *   - more runtime-only test engines...
   *   - junit-platform-isolator
   *   - junit-platform-isolator-worker
   * </code></pre>
   */
  NONE
}
