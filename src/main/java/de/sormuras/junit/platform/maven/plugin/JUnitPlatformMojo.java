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
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

/** Launch JUnit Platform Mojo. */
@org.apache.maven.plugins.annotations.Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
@org.codehaus.plexus.component.annotations.Component(role = AbstractMavenLifecycleParticipant.class)
public class JUnitPlatformMojo extends AbstractMavenLifecycleParticipant
    implements Mojo, ContextEnabled {

  /** Dry-run mode switch. */
  @Parameter(defaultValue = "false")
  private boolean dryRun;

  /** Customized file names. */
  @Parameter private FileNames fileNames = new FileNames();

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

  /** Override <strong>all</strong> JUnit Platform Console Launcher options. */
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

  @Parameter(defaultValue = "false")
  private boolean verbose;

  /**
   * Custom version map.
   *
   * @see Dependencies.Version
   */
  @Parameter private Map<String, String> versions = Map.of();

  private Log log;
  private Map pluginContext;

  public void setLog(Log log) {
    this.log = log;
  }

  public Log getLog() {
    if (this.log == null) {
      this.log = new SystemStreamLog();
    }

    return this.log;
  }

  public Map getPluginContext() {
    return this.pluginContext;
  }

  public void setPluginContext(Map pluginContext) {
    this.pluginContext = pluginContext;
  }

  @Override
  public void afterProjectsRead(MavenSession session) {
    debug("afterProjectsRead(%s)", session);

    for (var project : session.getProjects()) {

      debug("project = " + project.getName() + " // " + project);

      var thisPlugin =
          getPluginByGAFromContainer(
              "de.sormuras:junit-platform-maven-plugin", project.getModel().getBuild());

      if (thisPlugin == null) {
        continue;
      }

      var surefirePlugin =
          getPluginByGAFromContainer(
              "org.apache.maven.plugins:maven-surefire-plugin", project.getModel().getBuild());
      if (surefirePlugin != null) {
        surefirePlugin.getExecutions().clear();
      }

      getLog().info("thisPlugin = " + thisPlugin);

      var execution = new PluginExecution();
      execution.setId("injected-junit-platform-maven-plugin");
      execution.getGoals().add("launch-junit-platform");
      execution.setPhase("test");
      execution.setConfiguration(thisPlugin.getConfiguration());
      thisPlugin.getExecutions().add(execution);
    }
  }

  private Plugin getPluginByGAFromContainer(String ga, PluginContainer container) {
    Plugin result = null;
    for (Plugin plugin : container.getPlugins()) {
      debug(" - " + plugin + " // " + plugin.getGroupId() + ':' + plugin.getArtifactId());
      if (Objects.equals(ga, plugin.getGroupId() + ':' + plugin.getArtifactId())) {
        if (result != null) {
          throw new IllegalStateException("The build contains multiple versions of plugin " + ga);
        }
        result = plugin;
      }
    }
    return result;
  }

  void debug(String format, Object... args) {
    getLog().debug(String.format(format, args));
  }

  private void dumpParameters() {
    debug("");
    debug("Java module system");
    debug("  main -> %s", projectModules.toStringMainModule());
    debug("  test -> %s", projectModules.toStringTestModule());
    debug("  mode -> %s", projectModules.getMode());
    debug("File names");
    debug("  console-launcher.cmd.log = %s", fileNames.getConsoleLauncherCmdLog());
    debug("  console-launcher.err.log = %s", fileNames.getConsoleLauncherErrLog());
    debug("  console-launcher.out.log = %s", fileNames.getConsoleLauncherOutLog());
    debug("  module-info.test = %s", fileNames.getModuleInfoTest());
    debug("Versions");
    debug("  java.version = %s (%s)", System.getProperty("java.version"), Runtime.version());
    Dependencies.forEachVersion(v -> debug("  %s = %s", v.getKey(), version(v)));
    debug("Dependency path (short)");
    projectPaths.forEach(p -> debug("  %s", p.getFileName()));
    debug("Dependency path (full path)");
    projectPaths.forEach(p -> debug("  %s", p));
  }

  public void execute() throws MojoFailureException {
    getLog().info("Launching JUnit Platform...");

    if (skip) {
      getLog().info("JUnit Platform execution skipped.");
      return;
    }

    var mainPath = Paths.get(mavenBuild.getOutputDirectory());
    var testPath = Paths.get(mavenBuild.getTestOutputDirectory());
    projectModules = new Modules(mainPath, testPath);
    projectVersions = Dependencies.createArtifactVersionMap(this::getArtifactVersionOrNull);
    projectPaths = new Resolver(this).getPaths();

    if (getLog().isDebugEnabled()) {
      dumpParameters();
    }

    if (Files.notExists(testPath)) {
      getLog().info("Test output directory doesn't exist.");
      return;
    }

    int result = new Starter(this).getAsInt();
    if (result != 0) {
      throw new MojoFailureException("RED ALERT!");
    }
  }

  private String getArtifactVersionOrNull(String key) {
    var artifact = mavenProject.getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getBaseVersion();
  }

  FileNames getFileNames() {
    return fileNames;
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

  /**
   * Optional path to directory for storing reports.
   *
   * <p>The directory will be created if it does not exist. A relative path is resolved below the
   * current build directory, normally {@code target}. An empty path disables the generation of
   * reports.
   *
   * @return path to reports directory, may be empty
   */
  Optional<Path> getReportsPath() {
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

  boolean isVerbose() {
    return verbose;
  }

  /** Lookup version as a {@link String}. */
  String version(Dependencies.Version version) {
    var defaultVersion = projectVersions.get(version.getKey());
    return versions.getOrDefault(version.getKey(), defaultVersion);
  }
}
