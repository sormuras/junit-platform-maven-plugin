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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class JUnitPlatformMojoTests {

  @Test
  void checkDefaultPropertyValues() {
    JUnitPlatformMojo mojo = new JUnitPlatformMojo();

    assertFalse(mojo.isDryRun());
    assertEquals(Executor.DIRECT, mojo.getExecutor());
    assertEquals(Isolation.NONE, mojo.getIsolation());
    assertEquals(300L, mojo.getTimeout());

    assertNotNull(mojo.getLog());
    assertNull(mojo.getMavenProject());
    assertNull(mojo.getMavenRepositorySession());
    assertNull(mojo.getMavenResolver());

    JavaOptions javaOptions = mojo.getJavaOptions();
    assertFalse(javaOptions.inheritIO);
    assertEquals("", javaOptions.executable);
    assertEquals("", javaOptions.addModulesArgument);
    assertSame(Collections.EMPTY_LIST, javaOptions.additionalOptions);
    assertSame(Collections.EMPTY_LIST, javaOptions.overrideJavaOptions);
    assertSame(Collections.EMPTY_LIST, javaOptions.overrideLauncherOptions);
  }
}
