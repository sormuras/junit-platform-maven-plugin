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

import static de.sormuras.junit.platform.isolator.Version.JUNIT_PLATFORM_VERSION;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import de.sormuras.junit.platform.isolator.Configuration;
import de.sormuras.junit.platform.isolator.ConfigurationBuilder;
import de.sormuras.junit.platform.isolator.Driver;
import de.sormuras.junit.platform.isolator.Isolator;
import de.sormuras.junit.platform.isolator.Modules;
import de.sormuras.junit.platform.isolator.OverlaySingleton;
import de.sormuras.junit.platform.isolator.TestMode;
import de.sormuras.junit.platform.isolator.Version;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

/** Launch JUnit Platform Mojo. */
@org.apache.maven.plugins.annotations.Mojo(
    name = "launch",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
@org.codehaus.plexus.component.annotations.Component(role = AbstractMavenLifecycleParticipant.class)
public class JUnitPlatformMojo extends AbstractMavenLifecycleParticipant implements Mojo {

  /** Skip execution of this plugin. */
  @Parameter(defaultValue = "false", property = "junit-platform.skip")
  private boolean skip = false;

  /** Isolate artifacts in separated class loaders. */
  @Parameter(defaultValue = "NONE")
  private Isolation isolation = Isolation.NONE;

  /** Dry-run mode discovers tests but does not execute them. */
  @Parameter(defaultValue = "false")
  private boolean dryRun = false;

  /** Global timeout duration in seconds. */
  @Parameter(defaultValue = "300")
  private long timeout = 300L;

  /** Execution mode. */
  @Parameter(defaultValue = "DIRECT")
  private Executor executor = Executor.DIRECT;

  /** Customized Java command line options. */
  @Parameter private JavaOptions javaOptions = new JavaOptions();

  /** Tweak options to fine-tune test execution. */
  @Parameter private Tweaks tweaks = new Tweaks();

  /** Test discovery options. */
  @Parameter private Selectors selectors = new Selectors();

  /** Custom version map to override detected version. */
  @Parameter private Map<String, String> versions = emptyMap();

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

  /** The current Maven session. */
  @Parameter(defaultValue = "${session}", readonly = true)
  protected MavenSession mavenSession;

  /** The tool chain manager. */
  @Component private ToolchainManager mavenToolchainManager;

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
  @Parameter private Map<String, String> parameters = emptyMap();

  /**
   * Launcher class name patterns.
   *
   * <p>Provide regular expressions to include only classes whose fully qualified names match. To
   * avoid loading classes unnecessarily, the default pattern only includes class names that begin
   * with "Test" or end with "Test" or "Tests". Default: {@code ^(Test.*|.+[.$]Test.*|.*Tests?)$}
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --include-classname <String>}
   */
  @Parameter private Set<String> classNamePatterns;

  /**
   * Select class.
   *
   * <p>Provide a test to launch (useful in interactive mode).
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --select-class= <String>} if class can be resolved in outputTestDirectory or {@code
   * --include-classname=.*\\.<String>} if it can't.
   */
  @Parameter(property = "test") // property must stay short
  private String test; // todo: enable to not use fqn but just simple name of classes

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
  @Parameter(property = "tag")
  private Set<String> tags = emptySet();

  /** Base output directory path used by this plugin to store log files and reports. */
  @Parameter(defaultValue = "${project.build.directory}/junit-platform")
  private File targetDirectory;

  // internal
  @Parameter(readonly = true, defaultValue = "${mojoExecution}")
  private MojoExecution execution;

  /** The log instance passed in via setter. */
  private Log log;

  /** Modular world helper. */
  private Modules projectModules;

  /** Versions detected by scanning the artifacts of the current project. */
  private Map<String, String> projectVersions;

  @Override
  public void setLog(Log log) {
    this.log = log;
  }

  @Override
  public Log getLog() {
    if (this.log == null) {
      this.log = new SystemStreamLog();
    }
    return this.log;
  }

  void debug(String format, Object... args) {
    getLog().debug(formatMessage(format, args));
  }

  private void debug(String caption, Collection<String> paths) {
    debug(caption);
    paths.forEach(
        path -> debug(String.format("  %-50s -> %s", Paths.get(path).getFileName(), path)));
  }

  private void debug(String key, Artifact artifact) {
    debug(String.format("  %-50s -> %s trail=%s", key, artifact, artifact.getDependencyTrail()));
  }

  void info(String format, Object... args) {
    getLog().info(formatMessage(format, args));
  }

  void warn(String format, Object... args) {
    getLog().warn(formatMessage(format, args));
  }

  void error(String format, Object... args) {
    getLog().error(formatMessage(format, args));
  }

  private static String formatMessage(String pattern, Object... args) {
    // fast-path
    if (args.length == 0) {
      return pattern;
    }
    // guard against illegal patterns...
    try {
      return MessageFormat.format(pattern, args);
    } catch (IllegalArgumentException e) {
      return pattern + " " + Arrays.asList(args) + " // " + e.getClass() + ": " + e.getMessage();
    }
  }

  @Override
  public void afterProjectsRead(MavenSession session) {
    String group = "de.sormuras.junit";
    String artifact = "junit-platform-maven-plugin";
    for (MavenProject project : session.getProjects()) {
      findPlugin(project, group, artifact)
          .ifPresent(plugin -> injectThisPlugin(session, project, plugin));
    }
  }

  private void injectThisPlugin(MavenSession session, MavenProject project, Plugin thisPlugin) {
    PluginExecution execution = new PluginExecution();
    execution.setId("injected-launch");
    execution.getGoals().add("launch");
    execution.setPhase("test");
    execution.setConfiguration(thisPlugin.getConfiguration());
    thisPlugin.getExecutions().add(execution);

    String surefireGroup = "org.apache.maven.plugins";
    String surefireArtifact = "maven-surefire-plugin";
    findPlugin(project, surefireGroup, surefireArtifact)
        .ifPresent(surefire -> mangleSurefirePlugin(session, surefire, thisPlugin));
  }

  private void mangleSurefirePlugin(
      MavenSession session, Plugin surefirePlugin, Plugin junitPlugin) {
    // "-D...keep.executions=true" --> skip next code block: don't clear executions
    // "-D...keep.executions=false|<empty>" --> enter block:  clear executions
    if (!Boolean.getBoolean("junit-platform.surefire.keep.executions")) {
      surefirePlugin.getExecutions().clear();
    }

    new SurefireMigrationSupport(this, session).apply(surefirePlugin, junitPlugin);
  }

  private Optional<Plugin> findPlugin(MavenProject project, String group, String artifact) {
    List<Plugin> plugins = project.getModel().getBuild().getPlugins();
    return plugins.stream()
        .filter(plugin -> group.equals(plugin.getGroupId()))
        .filter(plugin -> artifact.equals(plugin.getArtifactId()))
        .reduce(
            (u, v) -> {
              throw new IllegalStateException("Plugin is not unique: " + artifact);
            });
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    debug("Executing JUnitPlatformMojo...");

    if (skip) {
      info("JUnit Platform Plugin execution skipped.");
      return;
    }

    if (mavenProject.getPackaging().equals("pom")) {
      info("JUnit Platform Plugin execution skipped: project uses 'pom' packaging");
      return;
    }

    MojoHelper mojoHelper = new MojoHelper(this, mavenSession, execution);
    mojoHelper.autoConfigure("javaOptions", javaOptions);
    mojoHelper.autoConfigure("tweaks", tweaks);

    Path mainPath = Paths.get(mavenBuild.getOutputDirectory());
    Path testPath = Paths.get(mavenBuild.getTestOutputDirectory());
    this.projectModules = new Modules(mainPath, testPath);
    this.projectVersions = Version.buildMap(this::artifactVersionOrNull);

    String moduleInfoTest =
        findModuleInfoTest(mavenBuild.getTestSourceDirectory(), testPath.toString());

    info("Launching JUnit Platform {0}...", version(JUNIT_PLATFORM_VERSION));
    if (getLog().isDebugEnabled()) {
      debug("Path");
      debug("  java.home = {0}", System.getProperty("java.home"));
      debug("  user.dir = {0}", System.getProperty("user.dir"));
      debug("  project.basedir = {0}", mavenProject.getBasedir());
      debug("Class Loader");
      debug("  mojo''s loader = {0}", getClass().getClassLoader());
      debug("  context loader = {0}", Thread.currentThread().getContextClassLoader());
      debug("  platform loader = {0}", OverlaySingleton.INSTANCE.platformClassLoader());
      debug("Artifact Map");
      mavenProject.getArtifactMap().keySet().stream()
          .sorted()
          .forEach(k -> debug(k, getMavenProject().getArtifactMap().get(k)));
      debug("Version");
      debug("  java.version = {0}", System.getProperty("java.version"));
      debug("  java.class.version = {0}", System.getProperty("java.class.version"));
      Version.forEach(v -> debug("  {0} = {1}", v.getKey(), version(v)));
      debug("Java Module System");
      debug("  main -> {0}", projectModules.toStringMainModule());
      debug("  test -> {0}", projectModules.toStringTestModule());
      debug("  mode -> {0}", projectModules.getMode());
      debug("  module-info.test -> {0}", moduleInfoTest);
    }

    // Create target directory to store log and report files...
    Path targetPath = targetDirectory.toPath();
    try {
      Files.createDirectories(targetPath);
    } catch (IOException e) {
      throw new MojoExecutionException("Can't create target path: " + targetPath, e);
    }

    MavenDriver driver = new MavenDriver(this);

    // Basic configuration...
    ConfigurationBuilder configurationBuilder =
        new ConfigurationBuilder()
            .setDryRun(isDryRun())
            .setFailIfNoTests(tweaks.failIfNoTests)
            .setDefaultAssertionStatus(tweaks.defaultAssertionStatus)
            .setPlatformClassLoader(tweaks.platformClassLoader)
            .setTargetDirectory(targetPath.toString())
            .setTargetMainPath(mainPath.toString())
            .setTargetTestPath(testPath.toString())
            .setWorkerIsolationRequired(tweaks.workerIsolationRequired)
            .setPaths(driver.buildPathMap(targetPath))
            .setModuleInfoTestPath(moduleInfoTest)
            .discovery()
            // selectors
            .setSelectedDirectories(selectors.directories)
            .setSelectedFiles(selectors.files)
            .setSelectedModules(selectors.modules)
            .setSelectedPackages(selectors.packages)
            .setSelectedClasses(selectors.classes)
            .setSelectedMethods(selectors.methods)
            .setSelectedClasspathResources(selectors.resources)
            .setSelectedUris(selectors.uris)
            // filters
            .setFilterClassNamePatterns(classNamePatterns)
            .setFilterTags(tags)
            // configuration parameters
            .setParameters(parameters)
            .end();

    // No custom selector configured?
    if (selectors.isEmpty()) {
      debug("No custom selector was configured, providing default one...");
      if (Files.notExists(testPath)) {
        if (tweaks.skipOnMissingTestOutputDirectory) {
          info("JUnit Platform Plugin execution skipped: test output directory does not exist.");
          return;
        }
        warn("Test output directory does not exist... this may lead to failures");
      }
      TestMode mode = projectModules.getMode();
      if (mode == TestMode.CLASSIC) {
        Set<String> roots = singleton(mavenBuild.getTestOutputDirectory());
        configurationBuilder.discovery().setSelectedClasspathRoots(roots);
      } else {
        String module =
            mode == TestMode.MODULAR_PATCHED_TEST_RUNTIME
                ? projectModules.getMainModuleName().orElseThrow(AssertionError::new)
                : projectModules.getTestModuleName().orElseThrow(AssertionError::new);
        Set<String> modules = singleton(module);
        configurationBuilder.discovery().setSelectedModules(modules);
      }
    }

    Configuration configuration = configurationBuilder.build();
    if (getLog().isDebugEnabled()) {
      debug("Isolator Path Layering");
      configuration.basic().getPaths().forEach(this::debug);
    }

    try {
      int result = execute(driver, configuration);
      if (result == 2) {
        throw new MojoFailureException("No tests found.");
      }
      if (result != 0) {
        throw new MojoFailureException("RED ALERT!");
      }
    } catch (MojoExecutionException | MojoFailureException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError("Unexpected exception caught!", e);
    }
  }

  private int execute(Driver driver, Configuration configuration) throws Exception {
    if (executor == Executor.DIRECT) {
      return executeDirect(driver, configuration);
    }
    if (executor == Executor.JAVA) {
      return executeJava(configuration);
    }
    throw new MojoExecutionException("Unsupported executor: " + executor);
  }

  private int executeDirect(Driver driver, Configuration configuration) throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Integer> future = executor.submit(() -> new Isolator(driver).evaluate(configuration));
    try {
      return future.get(timeout, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      warn("Global timeout of {0} second(s) reached.", timeout);
      throw new MojoFailureException("Global timeout reached.", e);
    } catch (Exception e) {
      throw new MojoExecutionException("Execution failed!", e);
    } finally {
      executor.shutdownNow();
    }
  }

  private int executeJava(Configuration configuration) {
    JavaExecutor executor = new JavaExecutor(this);
    return executor.evaluate(configuration);
  }

  String getTest() {
    return test;
  }

  Executor getExecutor() {
    return executor;
  }

  JavaOptions getJavaOptions() {
    return javaOptions;
  }

  Tweaks getTweaks() {
    return tweaks;
  }

  Modules getProjectModules() {
    return projectModules;
  }

  MavenProject getMavenProject() {
    return mavenProject;
  }

  MavenSession getMavenSession() {
    return mavenSession;
  }

  public MojoExecution getMojoExecution() {
    return execution;
  }

  RepositorySystemSession getMavenRepositorySession() {
    return mavenRepositorySession;
  }

  RepositorySystem getMavenResolver() {
    return mavenResolver;
  }

  long getTimeout() {
    return timeout;
  }

  boolean isDryRun() {
    return dryRun;
  }

  Isolation getIsolation() {
    return isolation;
  }

  private String artifactVersionOrNull(String key) {
    Artifact artifact = mavenProject.getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getBaseVersion();
  }

  String version(Version version) {
    String detectedVersion = projectVersions.get(version.getKey());
    return versions.getOrDefault(version.getKey(), detectedVersion);
  }

  @SafeVarargs
  final void removeExcludedArtifacts(Collection<String>... collections) {
    for (String exclude : tweaks.dependencyExcludes) {
      org.apache.maven.artifact.Artifact artifact = mavenProject.getArtifactMap().get(exclude);
      if (artifact == null) {
        debug("Can't exclude what isn't included: " + exclude);
        continue;
      }
      String excludedPath = artifact.getFile().toPath().toString();
      for (Collection<String> collection : collections) {
        collection.remove(excludedPath);
      }
    }
  }

  String getJavaExecutable() {
    Path java = Paths.get(getJavaExecutable(javaOptions.executable)).normalize().toAbsolutePath();
    if (!Files.isExecutable(java)) {
      warn("{0} is not executable", java);
    }
    return java.toString();
  }

  private String getJavaExecutable(String executable) {
    // User defined path executable
    if (executable != null && !executable.isEmpty()) {
      return executable;
    }
    // Ask toolchain
    Toolchain toolchain = mavenToolchainManager.getToolchainFromBuildContext("jdk", mavenSession);
    if (toolchain != null) {
      String java = toolchain.findTool("java");
      if (java != null) {
        return java;
      }
    }
    // Fall back to "java.home" system property
    String extension = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
    Path home = Paths.get(System.getProperty("java.home"));
    Path java = home.resolve("bin").resolve("java" + extension);
    return java.toString();
  }

  private static String findModuleInfoTest(String... roots) {
    for (String root : roots) {
      Path candidate = Paths.get(root).resolve("module-info.test");
      if (Files.isReadable(candidate)) {
        return candidate.toString();
      }
    }
    return "";
  }
}
