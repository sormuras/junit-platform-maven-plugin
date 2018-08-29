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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ModulesTests {

  @Test
  void automaticModuleNameStoredInMetaProperties() {
    var jar = Paths.get("target", "test-classes", "jars", "junit-platform-commons-1.2.0.jar");
    assertTrue(Files.isReadable(jar));

    var reference = Modules.getSingleModuleReferenceOrNull(jar);
    assertNotNull(reference);
    var descriptor = reference.descriptor();
    assertEquals("org.junit.platform.commons", descriptor.name());
    assertTrue(descriptor.isAutomatic());
  }

  @Test
  void explicitModuleName() {
    var jar = Paths.get("target", "test-classes", "jars", "slf4j-api-1.8.0-beta2.jar");
    assertTrue(Files.isReadable(jar));

    var reference = Modules.getSingleModuleReferenceOrNull(jar);
    assertNotNull(reference);
    var descriptor = reference.descriptor();
    assertEquals("org.slf4j", descriptor.name());
    assertFalse(descriptor.isAutomatic());
  }

  @Test
  void brokenModuleNameExtraction() {
    var jar = Paths.get("target", "test-classes", "jars", "broken-name_1.2.3-4.5.6.jar");
    assertTrue(Files.isReadable(jar));

    assertNull(Modules.getSingleModuleReferenceOrNull(jar));
  }
}
