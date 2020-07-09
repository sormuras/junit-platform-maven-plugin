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

import static java.util.Optional.ofNullable;

import java.util.stream.Stream;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

class SurefireMigrationSupport {

  private final JUnitPlatformMojo mojo;

  SurefireMigrationSupport(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
  }

  // TODO Parse Surefire configuration and pick more "interesting" settings...
  //      https://github.com/sormuras/junit-platform-maven-plugin/issues/23
  void apply(Plugin surefirePlugin, Plugin junitPlugin) {
    String support = System.getProperty("junit-platform.surefire.migration.support", "true");
    if (!Boolean.parseBoolean(support)) {
      return;
    }
    Object configuration = surefirePlugin.getConfiguration();
    if (configuration == null) { // No Surefire configuration element found
      return;
    }
    if (!(configuration instanceof Xpp3Dom)) { // Unlikely but to prevent future changes
      mojo.warn("Surefire configuration is not a Xpp3Dom, skipping migration: {0}", configuration);
      return;
    }

    Xpp3Dom surefireConfiguration = (Xpp3Dom) configuration;
    ofNullable(surefireConfiguration.getChild("systemPropertyVariables"))
        .filter(it -> it.getChildCount() > 0)
        .ifPresent(it -> migrateSystemPropertyVariables(it, junitPlugin));

    ofNullable(surefireConfiguration.getChild("environmentVariables"))
        .filter(it -> it.getChildCount() > 0)
        .ifPresent(it -> migrateEnvironmentVariables(it, junitPlugin));
  }

  private void migrateSystemPropertyVariables(Xpp3Dom dom, Plugin junitPlugin) {
    Xpp3Dom targetConfig = enforceConfiguration(junitPlugin);
    Xpp3Dom javaOptions = getOrCreateChild(targetConfig, "javaOptions");
    Xpp3Dom additionalOptions = getOrCreateChild(javaOptions, "additionalOptions");
    Stream.of(dom.getChildren())
        .map(
            it -> {
              Xpp3Dom option = new Xpp3Dom("additionalOption");
              option.setValue("-D" + it.getName() + "=" + it.getValue());
              return option;
            })
        .forEach(additionalOptions::addChild);
  }

  private void migrateEnvironmentVariables(Xpp3Dom dom, Plugin junitPlugin) {
    Xpp3Dom targetConfig = enforceConfiguration(junitPlugin);
    Xpp3Dom additionalEnvironment = getOrCreateChild(targetConfig, "additionalEnvironment");
    Stream.of(dom.getChildren()).forEach(additionalEnvironment::addChild);
  }

  private static Xpp3Dom enforceConfiguration(Plugin plugin) {
    Xpp3Dom targetConfig = (Xpp3Dom) plugin.getConfiguration();
    if (targetConfig == null) {
      targetConfig = new Xpp3Dom("configuration");
      plugin.setConfiguration(targetConfig);
    }
    return targetConfig;
  }

  private static Xpp3Dom getOrCreateChild(Xpp3Dom parent, String name) {
    Xpp3Dom child = parent.getChild(name);
    if (child == null) {
      child = new Xpp3Dom(name);
      parent.addChild(child);
    }
    return child;
  }
}
