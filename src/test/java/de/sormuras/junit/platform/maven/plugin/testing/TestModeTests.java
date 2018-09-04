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

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class TestModeTests {

  @Test
  void mode_0() {
    assertSame(TestMode.CLASSIC, TestMode.of(null, null));
  }

  @Test
  void mode_1() {
    assertSame(TestMode.MAIN_CLASSIC_TEST_MODULE, TestMode.of(null, "foo"));
    assertSame(TestMode.MAIN_CLASSIC_TEST_MODULE, TestMode.of(null, "bar"));
  }

  @Test
  void mode_2() {
    assertSame(TestMode.MAIN_MODULE_TEST_CLASSIC, TestMode.of("foo", null));
    assertSame(TestMode.MAIN_MODULE_TEST_CLASSIC, TestMode.of("bar", null));
  }

  @Test
  void mode_3() {
    assertSame(TestMode.MAIN_MODULE_TEST_MODULE_SAME_NAME, TestMode.of("foo", "foo"));
  }

  @Test
  void mode_4() {
    assertSame(TestMode.MODULAR, TestMode.of("foo", "bar"));
    assertSame(TestMode.MODULAR, TestMode.of("bar", "foo"));
  }
}
