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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;

/** Options consumed by the {@link JavaExecutor}. */
@SuppressWarnings("WeakerAccess")
public class JavaOptions {
  /**
   * This is the path to the {@code java} executable.
   *
   * <p>When this parameter is not set or empty, the plugin attempts to load a {@code jdk} toolchain
   * and use it to find the {@code java} executable. If no {@code jdk} toolchain is defined in the
   * project, the {@code java} executable is determined by the current {@code java.home} system
   * property, extended to {@code ${java.home}/bin/java[.exe]}.
   */
  String executable = "";

  /** Passed as {@code -Dfile.encoding=${encoding}, defaults to {@code UTF-8}. */
  String encoding = "UTF-8";

  /** Play nice with calling process. */
  boolean inheritIO = false;

  /**
   * Enable remote debugging on the JVM, can be set to true (port 5005), a port value or false
   * (disabled).
   */
  String debug = "false";

  /** Override <strong>all</strong> Java command line options. */
  List<String> overrideJavaOptions = emptyList();

  /** Override <strong>all</strong> JUnit Platform Console Launcher options. */
  List<String> overrideLauncherOptions = emptyList();

  /** Additional JUnit Platform Console Launcher options. */
  List<String> additionalLauncherOptions = emptyList();

  /** Additional Java command line options prepended to auto-generated options. */
  List<String> additionalOptions = emptyList();

  /** Environment set on the java process. */
  Map<String, String> additionalEnvironment = emptyMap();

  /** Argument for the {@code --add-modules} options: like {@code ALL-MODULE-PATH,ALL-DEFAULT}. */
  String addModulesArgument = "";
}
