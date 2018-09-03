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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractMojo {

  /** Dry-run mode switch. */
  @Parameter(defaultValue = "false")
  private boolean dryRun;

  /** System-specific path to the Java executable. */
  @Parameter private String javaExecutable;

  /** Customized Java command line options. */
  @Parameter private JavaOptions javaOptions = new JavaOptions();

  /** The underlying Maven build model. */
  @Parameter(defaultValue = "${project.build}", readonly = true, required = true)
  private Build mavenBuild;

  /** The underlying Maven project. */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject mavenProject;

  /** The current repository/network configuration of Maven. */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
  private RepositorySystemSession mavenRepositorySession;

  /** The entry point to Maven Artifact Resolver, i.e. the component doing all the work. */
  @Component private RepositorySystem mavenResolver;

  /** Override <strong>all</strong> Java command line options. */
  @Parameter private List<String> overrideJavaOptions;

  /** Override <strong>all</strong> JUnict Platform Console Launcher options. */
  @Parameter private List<String> overrideLauncherOptions;

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
  @Parameter private Map<String, String> parameters = Map.of();

  /** Module system helper. */
  private Modules projectModules;

  /** Elements of the class- or module-path. */
  private List<Path> projectPaths;

  /** Detected versions extracted from the project's dependencies. */
  private Map<String, String> projectVersions;

  /**
   * Directory for storing reports, like test result files, or empty to disable reports.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --reports-dir <String>}
   *
   * @see #getReportsPath()
   */
  @Parameter(defaultValue = "junit-platform/reports")
  private String reports;

  /**
   * Skip execution of this plugin.
   *
   * @see #isDryRun()
   */
  @Parameter(defaultValue = "false")
  private boolean skip;

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
  @Parameter private List<String> tags = List.of();

  /** Global timeout duration in seconds. */
  @Parameter(defaultValue = "100")
  private long timeout;

  /**
   * Custom version map.
   *
   * @see Dependencies.Version
   */
  @Parameter private Map<String, String> versions = Map.of();

  void debug(String format, Object... args) {
    getLog().debug(String.format(format, args));
  }

  public void execute() throws MojoFailureException {
    Log log = getLog();
    log.info("Launching JUnit Platform...");

    if (skip) {
      log.info(MessageUtils.buffer().warning("JUnit Platform execution skipped.").toString());
      return;
    }

    if (Files.notExists(Paths.get(mavenBuild.getTestOutputDirectory()))) {
      log.info(MessageUtils.buffer().warning("Test output directory doesn't exist.").toString());
      return;
    }

    var mainPath = Paths.get(mavenBuild.getOutputDirectory());
    var testPath = Paths.get(mavenBuild.getTestOutputDirectory());
    projectModules = new Modules(mainPath, testPath);
    projectVersions = Dependencies.createArtifactVersionMap(this::getArtifactVersionOrNull);
    projectPaths = new Resolver(this).getPaths();

    debug("");
    debug("Java module system");
    debug("  main -> %s", projectModules.toStringMainModule());
    debug("  test -> %s", projectModules.toStringTestModule());
    debug("  mode -> %s", projectModules.getMode());
    debug("Detected versions");
    Dependencies.forEachVersion(v -> debug("  %s = %s", v.getKey(), version(v)));
    debug("Dependency path (short)");
    projectPaths.forEach(p -> debug("  %s", p.getFileName()));
    debug("Dependency path (full path)");
    projectPaths.forEach(p -> debug("  %s", p));

    int result = new JUnitPlatformStarter(this).getAsInt();
    if (result != 0) {
      throw new MojoFailureException("RED ALERT!");
    }
  }

  private String getArtifactVersionOrNull(String key) {
    var artifact = mavenProject.getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getVersion();
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

  MavenProject getMavenProject() {
    return mavenProject;
  }

  RepositorySystemSession getMavenRepositorySession() {
    return mavenRepositorySession;
  }

  RepositorySystem getMavenResolver() {
    return mavenResolver;
  }

  Optional<List<String>> getOverrideJavaOptions() {
    return Optional.ofNullable(overrideJavaOptions);
  }

  Optional<List<String>> getOverrideLauncherOptions() {
    return Optional.ofNullable(overrideLauncherOptions);
  }

  Map<String, String> getParameters() {
    return parameters;
  }

  Modules getProjectModules() {
    return projectModules;
  }

  List<Path> getProjectPaths() {
    return projectPaths;
  }

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
    return Optional.of(Paths.get(mavenBuild.getDirectory()).resolve(path));
  }

  List<String> getTags() {
    return tags;
  }

  Duration getTimeout() {
    return Duration.ofSeconds(timeout);
  }

  boolean isDryRun() {
    return dryRun;
  }

  /** Lookup version as a {@link String}. */
  String version(Dependencies.Version version) {
    var defaultVersion = projectVersions.get(version.getKey());
    return versions.getOrDefault(version.getKey(), defaultVersion);
  }
}
