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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DependenciesTests {

  /** Expected size of the {@link Dependencies.Version} enum. */
  private static final int SIZE = 3;

  @Test
  void forEachVersionCallsThePassedVisitorThreeTimes() {
    List<Dependencies.Version> versions = new ArrayList<>();
    Dependencies.forEachVersion(versions::add);
    assertEquals(SIZE, versions.size(), "versions=" + versions);
  }

  @Test
  void passingNullAsVersionToCreateArtifactVersionMap() {
    Map<String, String> map = Dependencies.createArtifactVersionMap(__ -> null);
    assertEquals(SIZE, map.size(), "map=" + map);
    assertEquals("1.3.2", map.get("junit.platform.version"));
    assertEquals("5.3.2", map.get("junit.jupiter.version"));
    assertEquals("5.3.2", map.get("junit.vintage.version"));
  }

  @Test
  void passingDashAsVersionToCreateArtifactVersionMap() {
    Map<String, String> map = Dependencies.createArtifactVersionMap(__ -> "-");
    assertEquals(SIZE, map.size(), "map=" + map);
    assertEquals("-", map.get("junit.platform.version"));
    assertEquals("-", map.get("junit.jupiter.version"));
    assertEquals("-", map.get("junit.vintage.version"));
  }

  @Test
  void passingPresetVersionMapToCreateArtifactVersionMap() {
    Map<String, String> preset = new HashMap<>();
    preset.put("org.junit.platform:junit-platform-commons", "a");
    preset.put("org.junit.jupiter:junit-jupiter-api", "b");
    preset.put("org.junit.vintage:junit-vintage-engine", "c");
    Map<String, String> map = Dependencies.createArtifactVersionMap(preset::get);
    assertEquals(SIZE, map.size(), "map=" + map);
    assertEquals("a", map.get("junit.platform.version"));
    assertEquals("b", map.get("junit.jupiter.version"));
    assertEquals("c", map.get("junit.vintage.version"));
  }
}
