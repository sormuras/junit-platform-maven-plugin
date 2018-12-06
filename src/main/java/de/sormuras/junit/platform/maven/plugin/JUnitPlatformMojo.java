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

import static de.sormuras.junit.platform.maven.plugin.Dependencies.Version.JUNIT_PLATFORM_VERSION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
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
import org.eclipse.aether.resolution.ArtifactResolutionException;

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
    getLog().debug(String.format(format, args));
  }

  void debug(String caption, Collection<Path> paths) {
    debug(caption);
    paths.forEach(path -> debug("  %-50s -> %s", path.getFileName(), path));
  }

  void info(String format, Object... args) {
    getLog().info(String.format(format, args));
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
      info("JUnit Platform execution skipped.");
      return;
    }

    Dependencies dependencies = new Dependencies(this);

    info("Launching JUnit Platform " + dependencies.version(JUNIT_PLATFORM_VERSION) + "...");
    if (getLog().isDebugEnabled()) {
      debug("Paths");
      debug("  java.home = %s", System.getProperty("java.home"));
      debug("  user.dir = %s", System.getProperty("user.dir"));
      debug("  ${project.basedir} = %s", mavenProject.getBasedir());
      debug("  class loader parents = %s", walk(getClass().getClassLoader()));
      debug("${project.artifactMap}");
      mavenProject.getArtifactMap().forEach((k, a) -> debug("  %-50s -> %s", k, a));
      debug("Versions");
      debug("  java.version = %s", System.getProperty("java.version"));
      debug("  java.class.version = %s", System.getProperty("java.class.version"));
      Dependencies.forEachVersion(v -> debug("  %s = %s", v.getKey(), dependencies.version(v)));
    }

    if (Files.notExists(Paths.get(mavenBuild.getTestOutputDirectory()))) {
      info("Test output directory doesn't exist.");
      return;
    }

    Resolver resolver = new Resolver(this);
    Isolator isolator = new Isolator(this, resolver);
    try {
      ClassLoader loader = isolator.createClassLoader();
      debug("execution class loader parents = %s", walk(loader));
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoFailureException("Resolution required!", e);
    } catch (ArtifactResolutionException e) {
      throw new MojoFailureException("Artifact resolution failed!", e);
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

  private static String walk(ClassLoader loader) {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    if (loader == tccl) {
      return "TCCL(" + loader + ")";
    }
    return "Loader(" + loader + ") -> " + walk(loader.getParent());
  }
}
