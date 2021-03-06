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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class TweaksTests {

  @Test
  void checkDefaultPropertyValues() {
    Tweaks tweaks = new Tweaks();
    assertTrue(tweaks.defaultAssertionStatus);
    assertTrue(tweaks.failIfNoTests);
    assertTrue(tweaks.platformClassLoader);
    assertSame(Collections.EMPTY_LIST, tweaks.additionalLauncherDependencies);
    assertSame(Collections.EMPTY_LIST, tweaks.additionalLauncherPathElements);
    assertSame(Collections.EMPTY_LIST, tweaks.additionalTestDependencies);
    assertSame(Collections.EMPTY_LIST, tweaks.additionalTestPathElements);
    assertSame(Collections.EMPTY_LIST, tweaks.dependencyExcludes);
  }
}
