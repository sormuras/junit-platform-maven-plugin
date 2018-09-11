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

import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_JUPITER_API;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_JUPITER_ENGINE;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_PLATFORM_COMMONS;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_VINTAGE_ENGINE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Dependency resolution helper. */
class Dependencies {

  static Map<String, String> createArtifactVersionMap(UnaryOperator<String> versionOperator) {
    var result = new HashMap<String, String>();
    withNextVersion:
    for (var version : Version.values()) {
      var versionKey = version.getKey();
      for (var groupArtifact : version.getGroupArtifacts()) {
        var artifactVersion = versionOperator.apply(groupArtifact.toIdentifier());
        if (artifactVersion != null) {
          result.put(versionKey, artifactVersion);
          continue withNextVersion;
        }
      }
      result.put(versionKey, version.getDefaultVersion());
    }
    return result;
  }

  static void forEachVersion(Consumer<Version> versionConsumer) {
    Arrays.stream(Version.values()).sorted().forEach(versionConsumer);
  }

  /** Maven group and artifact coordinates. */
  enum GroupArtifact {
    JUNIT_JUPITER_API("org.junit.jupiter", "junit-jupiter-api", "JUNIT_JUPITER"),

    JUNIT_JUPITER_ENGINE("org.junit.jupiter", "junit-jupiter-engine", "JUNIT_JUPITER"),

    JUNIT_PLATFORM_COMMONS("org.junit.platform", "junit-platform-commons", "JUNIT_PLATFORM"),

    JUNIT_PLATFORM_CONSOLE("org.junit.platform", "junit-platform-console", "JUNIT_PLATFORM"),

    JUNIT_VINTAGE_ENGINE("org.junit.vintage", "junit-vintage-engine", "JUNIT_VINTAGE");

    private final String artifact;
    private final String group;
    private final String versionName;

    GroupArtifact(String group, String artifact, String versionBaseName) {
      this.group = group;
      this.artifact = artifact;
      this.versionName = versionBaseName + "_VERSION";
    }

    String getArtifact() {
      return artifact;
    }

    String getGroup() {
      return group;
    }

    Version getVersion() {
      return Version.valueOf(versionName); // lazy
    }

    String toIdentifier() {
      return getGroup() + ':' + getArtifact();
    }
  }

  /** Maven artifact version defaults. */
  enum Version {
    JUNIT_JUPITER_VERSION("5.3.1", JUNIT_JUPITER_API, JUNIT_JUPITER_ENGINE),

    JUNIT_PLATFORM_VERSION("1.3.1", JUNIT_PLATFORM_COMMONS),

    JUNIT_VINTAGE_VERSION("5.3.1", JUNIT_VINTAGE_ENGINE);

    private final String key;
    private final String defaultVersion;
    private final List<GroupArtifact> groupArtifacts;

    Version(String defaultVersion, GroupArtifact... groupArtifacts) {
      this.key = name().toLowerCase().replace('_', '.');
      this.defaultVersion = defaultVersion;
      this.groupArtifacts = List.of(groupArtifacts);
    }

    List<GroupArtifact> getGroupArtifacts() {
      return groupArtifacts;
    }

    String getKey() {
      return key;
    }

    String getDefaultVersion() {
      return defaultVersion;
    }
  }
}
