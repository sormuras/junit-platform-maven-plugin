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
import java.util.List;
import java.util.Optional;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.artifact.Artifact;
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

  /** Dry-run mode discovers tests but does not execute them. */
  @Parameter(defaultValue = "false")
  private boolean dryRun;

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

  /**
   * Skip execution of this plugin.
   *
   * @see #isDryRun()
   */
  @Parameter(defaultValue = "false")
  private boolean skip;

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

  void debug(String format, Object... args) {
    getLog().debug(String.format(format, args));
  }

  @Override
  public void execute() throws MojoFailureException {
    debug("Executing JUnitPlatformMojo...");

    if (skip) {
      getLog().info("JUnit Platform execution skipped.");
      return;
    }

    Path testPath = Paths.get(mavenBuild.getTestOutputDirectory());

    getLog().info("Launching JUnit Platform...");
    if (getLog().isDebugEnabled()) {
      debug("  testPath %s", testPath);
    }

    if (Files.notExists(testPath)) {
      getLog().info("Test output directory doesn't exist.");
      return;
    }
  }

  private String getArtifactVersionOrNull(String key) {
    Artifact artifact = mavenProject.getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getBaseVersion();
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

  boolean isDryRun() {
    return dryRun;
  }
}
