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

  /** Detected versions extracted from the project's dependencies. */
  private Map<String, String> detectedVersions;

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

  /** Module system helper. */
  private Modules modules;

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
    modules = new Modules(mainPath, testPath);
    detectedVersions = Dependencies.createArtifactVersionMap(this::getArtifactVersionOrNull);

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

  private String getArtifactVersionOrNull(String key) {
    var artifact = mavenProject.getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getVersion();
  }

  String getDetectedVersion(String key) {
    return detectedVersions.get(key);
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

  Modules getModules() {
    return modules;
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
  String getVersion(String key) {
    return versions.getOrDefault(key, getDetectedVersion(key));
  }

  /** Dry-run mode switch. */
  boolean isDryRun() {
    return dryRun;
  }

  void resolve(List<String> elements, String groupAndArtifact, String version) throws Exception {
    var map = mavenProject.getArtifactMap();
    if (map.containsKey(groupAndArtifact)) {
      debug("Skip resolving '%s', because it is already mapped.", groupAndArtifact);
      return;
    }
    var gav = groupAndArtifact + ":" + version;
    debug("");
    debug("Resolving '%s' and its transitive dependencies...", gav);
    for (var resolved : resolve(gav)) {
      var key = resolved.getGroupId() + ':' + resolved.getArtifactId();
      if (map.containsKey(key)) {
        // debug("  X %s // mapped by project", resolved);
        continue;
      }
      var path = resolved.getFile().toPath().toAbsolutePath().normalize();
      var element = path.toString();
      if (elements.contains(element)) {
        // debug("  X %s // already added", resolved);
        continue;
      }
      debug(" -> %s", element);
      elements.add(element);
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
}
