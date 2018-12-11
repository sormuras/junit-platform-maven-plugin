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

import de.sormuras.junit.platform.isolator.Configuration;
import de.sormuras.junit.platform.isolator.Isolator;
import de.sormuras.junit.platform.isolator.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
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
public class JUnitPlatformMojo extends AbstractMavenLifecycleParticipant implements Mojo {

  /** Skip execution of this plugin. */
  @Parameter(defaultValue = "false")
  private boolean skip;

  /** Isolate artifacts in separated class loaders. */
  @Parameter(defaultValue = "true")
  private boolean isolate;

  /** Dry-run mode discovers tests but does not execute them. */
  @Parameter(defaultValue = "false")
  private boolean dryRun;

  /** Custom version map. */
  @Parameter private Map<String, String> versions = Collections.emptyMap();

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

  /** The log instance passed in via setter. */
  private Log log;

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
    getLog().debug(MessageFormat.format(format, args));
  }

  private void debug(String caption, Collection<Path> paths) {
    debug(caption);
    paths.forEach(path -> debug(String.format("  %-50s -> %s", path.getFileName(), path)));
  }

  void info(String format, Object... args) {
    getLog().info(MessageFormat.format(format, args));
  }

  void warn(String format, Object... args) {
    getLog().warn(MessageFormat.format(format, args));
  }

  @Override
  public void afterProjectsRead(MavenSession session) {
    for (MavenProject project : session.getProjects()) {
      Optional<Plugin> thisPlugin =
          findPlugin(project, "de.sormuras", "junit-platform-maven-plugin");
      thisPlugin.ifPresent(plugin -> injectThisPluginIntoTestExecutionPhase(project, plugin));
    }
  }

  private void injectThisPluginIntoTestExecutionPhase(MavenProject project, Plugin thisPlugin) {
    PluginExecution execution = new PluginExecution();
    execution.setId("injected-junit-platform-maven-plugin");
    execution.getGoals().add("launch-junit-platform");
    execution.setPhase("test");
    execution.setConfiguration(thisPlugin.getConfiguration());
    thisPlugin.getExecutions().add(execution);

    Optional<Plugin> surefirePlugin =
        findPlugin(project, "org.apache.maven.plugins", "maven-surefire-plugin");
    surefirePlugin.ifPresent(surefire -> surefire.getExecutions().clear());
  }

  private Optional<Plugin> findPlugin(MavenProject project, String group, String artifact) {
    List<Plugin> plugins = project.getModel().getBuild().getPlugins();
    return plugins
        .stream()
        .filter(plugin -> group.equals(plugin.getGroupId()))
        .filter(plugin -> artifact.equals(plugin.getArtifactId()))
        .reduce(
            (u, v) -> {
              throw new IllegalStateException("Plugin is not unique: " + artifact);
            });
  }

  @Override
  public void execute() throws MojoFailureException {
    debug("Executing JUnitPlatformMojo...");

    if (skip) {
      info("JUnit Platform Plugin execution skipped.");
      return;
    }

    // Configuration
    Set<String> set = new HashSet<>();
    set.add(mavenBuild.getTestOutputDirectory());
    Configuration configuration =
        new Configuration()
            .setWorkerCoordinates(
                "com.github.sormuras.junit-platform-isolator:junit-platform-isolator-worker:v1.0.0-M1")
            .setDryRun(isDryRun())
            .setSelectedClassPathRoots(set);
    MavenDriver driver = new MavenDriver(this, configuration);

    info("Launching JUnit Platform " + driver.version(JUNIT_PLATFORM_VERSION) + "...");
    if (getLog().isDebugEnabled()) {
      debug("Paths");
      debug("  java.home = {0}", System.getProperty("java.home"));
      debug("  user.dir = {0}", System.getProperty("user.dir"));
      debug("  project.basedir = {0}", mavenProject.getBasedir());
      debug("  class loader = {0}", getClass().getClassLoader());
      debug("  context loader = {0}", Thread.currentThread().getContextClassLoader());
      debug("Artifacts");
      mavenProject
          .getArtifactMap()
          .keySet()
          .stream()
          .sorted()
          .forEach(
              k -> debug(String.format("  %-50s -> %s", k, mavenProject.getArtifactMap().get(k))));
      debug("Versions");
      debug("  java.version = {0}", System.getProperty("java.version"));
      debug("  java.class.version = {0}", System.getProperty("java.class.version"));
      Version.forEach(v -> debug("  {0} = {1}", v.getKey(), driver.version(v)));
    }

    if (Files.notExists(Paths.get(mavenBuild.getTestOutputDirectory()))) {
      info("Test output directory does not exist.");
      return;
    }

    if (getLog().isDebugEnabled()) {
      debug("Paths");
      driver.paths().forEach(this::debug);
    }

    try {
      Isolator isolator = new Isolator(driver);
      int exitCode = isolator.evaluate(configuration);
      info("Manager returned {0}", exitCode);
    } catch (Exception e) {
      throw new MojoFailureException("Calling manager failed!", e);
    }
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

  Map<String, String> getVersions() {
    return versions;
  }

  boolean isDryRun() {
    return dryRun;
  }

  boolean isIsolate() {
    return isolate;
  }
}
