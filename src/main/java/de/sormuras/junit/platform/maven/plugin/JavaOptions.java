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

import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

@SuppressWarnings("WeakerAccess")
public class JavaOptions {
  /** Play nice with calling process. */
  @Parameter boolean inheritIO = false;

  /** Override <strong>all</strong> Java command line options. */
  @Parameter List<String> overrideJavaOptions = emptyList();

  /** Override <strong>all</strong> JUnit Platform Console Launcher options. */
  @Parameter List<String> overrideLauncherOptions = emptyList();

  /** Additional Java command line options prepended to auto-generated options. */
  @Parameter List<String> additionalOptions = emptyList();

  /** Argument for the {@code --add-modules} options: like {@code ALL-MODULE-PATH,ALL-DEFAULT}. */
  @Parameter String addModulesArgument = "";
}
