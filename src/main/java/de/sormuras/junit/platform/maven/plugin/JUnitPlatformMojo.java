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

import static de.sormuras.junit.platform.maven.plugin.Dependencies.Version.JUNIT_JUPITER_VERSION;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.Version.JUNIT_PLATFORM_VERSION;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.Version.JUNIT_VINTAGE_VERSION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractMojo {

  @Parameter(defaultValue = "false")
  private boolean dryRun;

  @Parameter private String javaExecutable;

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

  @Parameter private List<String> overrideJavaOptions;

  @Parameter private List<String> overrideLauncherOptions;

  @Parameter private Map<String, String> parameters = Map.of();

  /** Module system helper. */
  private Modules projectModules;

  /** Detected versions extracted from the project's dependencies. */
  private Map<String, String> projectVersions;

  /** Elements of the class- or module-path. */
  private List<Path> projectPaths;

  @Parameter(defaultValue = "junit-platform/reports")
  private String reports;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter private List<String> tags = List.of();

  @Parameter(defaultValue = "100")
  private long timeout;

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
    projectPaths = resolvePath();

    debug("");
    debug("Java module system");
    debug("  main -> %s", projectModules.toStringMainModule());
    debug("  test -> %s", projectModules.toStringTestModule());
    debug("  mode -> %s", projectModules.getMode());
    debug("Detected versions");
    Dependencies.forEachVersion(v -> debug("  %s = %s", v.getKey(), getVersion(v)));
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

  Modules getProjectModules() {
    return projectModules;
  }

  List<Path> getProjectPaths() {
    return projectPaths;
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
    return Optional.of(Paths.get(mavenBuild.getDirectory()).resolve(path));
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

  /** Desired version. */
  String getVersion(Dependencies.Version version) {
    var defaultVersion = projectVersions.get(version.getKey());
    return versions.getOrDefault(version.getKey(), defaultVersion);
  }

  /** Dry-run mode switch. */
  boolean isDryRun() {
    return dryRun;
  }

  private void resolve(List<Path> paths, String groupAndArtifact, String version) throws Exception {
    var map = mavenProject.getArtifactMap();
    if (map.containsKey(groupAndArtifact)) {
      debug("Skip resolving '%s', because it is already mapped.", groupAndArtifact);
      return;
    }
    var gav = groupAndArtifact + ":" + version;
    // debug("");
    // debug("Resolving '%s' and its transitive dependencies...", gav);
    for (var resolved : resolve(gav)) {
      var key = resolved.getGroupId() + ':' + resolved.getArtifactId();
      if (map.containsKey(key)) {
        // debug("  X %s // mapped by project", resolved);
        continue;
      }
      var path = resolved.getFile().toPath().toAbsolutePath().normalize();
      if (paths.contains(path)) {
        // debug("  X %s // already added", resolved);
        continue;
      }
      // debug(" -> %s", path);
      paths.add(path);
    }
  }

  private List<Artifact> resolve(String coordinates) throws Exception {
    var repositories = mavenProject.getRemotePluginRepositories();
    var artifact = new DefaultArtifact(coordinates);
    // debug("Resolving artifact %s from %s...", artifact, repositories);
    var artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(repositories);
    // var resolved = mavenResolver.resolveArtifact(mavenRepositorySession, artifactRequest);
    // debug("Resolved %s from %s", artifact, resolved.getRepository());
    // debug("Stored %s to %s", artifact, resolved.getArtifact().getFile());
    var collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(repositories);
    var dependencyRequest = new DependencyRequest(collectRequest, (all, ways) -> true);
    // debug("Resolving dependencies %s...", dependencyRequest);
    return mavenResolver
        .resolveDependencies(mavenRepositorySession, dependencyRequest)
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        // .peek(a -> debug("Artifact %s resolved to %s", a, a.getFile()))
        .collect(Collectors.toList());
  }

  List<Path> resolvePath() {
    var paths = new ArrayList<Path>();
    // test out
    paths.add(Paths.get(mavenProject.getBuild().getTestOutputDirectory()));
    // main out
    paths.add(Paths.get(mavenProject.getBuild().getOutputDirectory()));
    // deps from pom
    for (var artifact : mavenProject.getArtifacts()) {
      if (!artifact.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }
      var file = artifact.getFile();
      if (file != null) {
        paths.add(file.toPath());
      }
    }
    try {
      // deps from here (auto-complete)
      var map = mavenProject.getArtifactMap();
      // junit-jupiter-engine
      var jupiterApi = map.get("org.junit.jupiter:junit-jupiter-api");
      var jupiterEngine = "org.junit.jupiter:junit-jupiter-engine";
      if (jupiterApi != null && !map.containsKey(jupiterEngine)) {
        resolve(paths, jupiterEngine, getVersion(JUNIT_JUPITER_VERSION));
      }
      // junit-vintage-engine
      var vintageApi = map.get("junit:junit");
      var vintageEngine = "org.junit.vintage:junit-vintage-engine";
      if (vintageApi != null && !map.containsKey(vintageEngine)) {
        if (vintageApi.getVersion().equals("4.12")) {
          resolve(paths, vintageEngine, getVersion(JUNIT_VINTAGE_VERSION));
        }
      }
      // junit-platform-console
      var platformConsole = "org.junit.platform:junit-platform-console";
      if (!map.containsKey(platformConsole)) {
        resolve(paths, platformConsole, getVersion(JUNIT_PLATFORM_VERSION));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Resolving path elements failed", e);
    }
    // Done.
    return List.copyOf(paths);
  }
}
