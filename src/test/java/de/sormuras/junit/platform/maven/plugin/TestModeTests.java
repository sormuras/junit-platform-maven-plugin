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

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class TestModeTests {

  @Test
  void classic() {
    assertSame(TestMode.CLASSIC, TestMode.of(null, null));
    assertSame(TestMode.CLASSIC, TestMode.of(null, ""));
    assertSame(TestMode.CLASSIC, TestMode.of("", null));
    assertSame(TestMode.CLASSIC, TestMode.of("", ""));
  }

  @Test
  void modular() {
    assertSame(TestMode.MODULAR, TestMode.of(null, "foo"));
    assertSame(TestMode.MODULAR, TestMode.of(null, "bar"));
    assertSame(TestMode.MODULAR, TestMode.of("", "foo"));
    assertSame(TestMode.MODULAR, TestMode.of("", "bar"));
    assertSame(TestMode.MODULAR, TestMode.of("foo", "bar"));
    assertSame(TestMode.MODULAR, TestMode.of("bar", "foo"));
  }

  @Test
  void modularPatchedTestCompile() {
    assertSame(TestMode.MODULAR_PATCHED_TEST_COMPILE, TestMode.of("foo", "foo"));
    assertSame(TestMode.MODULAR_PATCHED_TEST_COMPILE, TestMode.of("bar", "bar"));
  }

  @Test
  void modularPatchedTestRuntime() {
    assertSame(TestMode.MODULAR_PATCHED_TEST_RUNTIME, TestMode.of("foo", null));
    assertSame(TestMode.MODULAR_PATCHED_TEST_RUNTIME, TestMode.of("bar", null));
    assertSame(TestMode.MODULAR_PATCHED_TEST_RUNTIME, TestMode.of("foo", ""));
    assertSame(TestMode.MODULAR_PATCHED_TEST_RUNTIME, TestMode.of("bar", ""));
  }
}
