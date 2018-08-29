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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.logging.MessageUtils;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractBaseMojo {

  @Parameter(defaultValue = "false")
  private boolean dryRun;

  @Parameter private String javaExecutable;

  @Parameter private JavaOptions javaOptions = new JavaOptions();

  @Parameter private List<String> overrideJavaOptions;

  @Parameter private List<String> overrideLauncherOptions;

  @Parameter private Map<String, String> parameters = Map.of();

  @Parameter(defaultValue = "junit-platform/reports")
  private String reports;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter private List<String> tags = List.of();

  @Parameter(defaultValue = "100")
  private long timeout;

  @Parameter private Map<String, String> versions = Map.of();

  public void execute() throws MojoFailureException {
    Log log = getLog();
    log.info("Launching JUnit Platform...");

    if (skip) {
      log.info(MessageUtils.buffer().warning("JUnit Platform execution skipped.").toString());
      return;
    }

    if (Files.notExists(Paths.get(getMavenProject().getBuild().getTestOutputDirectory()))) {
      log.info(MessageUtils.buffer().warning("Test output directory doesn't exist.").toString());
      return;
    }

    initialize();

    log.debug("");
    log.debug("JUnit-related versions");
    log.debug("  Platform  -> " + getJUnitPlatformVersion());
    log.debug("  Jupiter   -> " + getJUnitJupiterVersion());
    log.debug("  Vintage   -> " + getJUnitVintageVersion());
    log.debug("Java module system");
    log.debug("  main -> " + getModules().toStringMainModule());
    log.debug("  test -> " + getModules().toStringTestModule());
    log.debug("  mode -> " + getModules().getMode());

    int result = new JUnitPlatformStarter(this).getAsInt();
    if (result != 0) {
      throw new MojoFailureException("RED ALERT!");
    }
  }

  String getJavaExecutable() {
    if (javaExecutable != null) {
      return javaExecutable;
    }
    var path = ProcessHandle.current().info().command().map(Paths::get).orElseThrow();
    return path.normalize().toAbsolutePath().toString();
  }

  JavaOptions getJavaOptions() {
    return javaOptions;
  }

  Optional<List<String>> getOverrideJavaOptions() {
    return Optional.ofNullable(overrideJavaOptions);
  }

  Optional<List<String>> getOverrideLauncherOptions() {
    return Optional.ofNullable(overrideLauncherOptions);
  }

  /**
   * Launcher configuration parameters.
   *
   * <p>Set a configuration parameter for test discovery and execution.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --config <key=value>}
   *
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params">Configuration
   *     Parameters</a>
   */
  Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * Directory for storing reports, like test result files.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --reports-dir <String>}
   *
   * @return path to reports directory, may be empty
   * @see #getReportsPath()
   */
  String getReports() {
    return reports;
  }

  /**
   * Optional path to directory for storing reports.
   *
   * <p>The directory will be created if it does not exist. A relative path is resolved below the
   * current build directory, normally {@code target}. An empty path disables the generation of
   * reports.
   *
   * @return path to reports directory, may be empty
   * @see #getReports()
   */
  Optional<Path> getReportsPath() {
    var reports = getReports();
    if (reports.trim().isEmpty()) { // .isBlank()
      return Optional.empty();
    }
    Path path = Paths.get(reports);
    if (path.isAbsolute()) {
      return Optional.of(path);
    }
    return Optional.of(Paths.get(getMavenProject().getBuild().getDirectory()).resolve(path));
  }

  /**
   * Tags or tag expressions to include only tests whose tags match.
   *
   * <p>All tags and expressions will be combined using {@code OR} semantics.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --include-tag <String>}
   *
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions">Tag
   *     Expressions</a>
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering">Tagging
   *     and Filtering</a>
   */
  List<String> getTags() {
    return tags;
  }

  /**
   * Global timeout duration in seconds.
   *
   * @return timeout duration in seconds
   */
  Duration getTimeout() {
    return Duration.ofSeconds(timeout);
  }

  /** Desired JUnit Jupiter version. */
  String getJUnitJupiterVersion() {
    return getVersion("junit.jupiter.version");
  }

  /** Desired JUnit Platform version. */
  String getJUnitPlatformVersion() {
    return getVersion("junit.platform.version");
  }

  /** Desired JUnit Vintage version. */
  String getJUnitVintageVersion() {
    return getVersion("junit.vintage.version");
  }

  /** Desired version. */
  String getVersion(String key) {
    return versions.getOrDefault(key, getDetectedVersion(key));
  }

  /** Dry-run mode switch. */
  boolean isDryRun() {
    return dryRun;
  }
}
