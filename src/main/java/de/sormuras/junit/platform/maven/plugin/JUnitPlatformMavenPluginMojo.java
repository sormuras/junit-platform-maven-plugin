/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.sormuras.junit.platform.maven.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMavenPluginMojo extends AbstractMojo implements Configuration {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "100")
  private long timeout;

  @Parameter private Map<String, String> parameters = new HashMap<>();

  @Parameter(defaultValue = "true")
  private boolean strict;

  @Parameter(defaultValue = "junit-platform-reports")
  private String reports;

  @Parameter(readonly = true)
  private List<String> tags = new ArrayList<>();

  @Override
  public MavenProject getMavenProject() {
    return project;
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofSeconds(timeout);
  }

  @Override
  public boolean isStrict() {
    return strict;
  }

  @Override
  public String getReports() {
    return reports;
  }

  @Override
  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public List<String> getTags() {
    return tags;
  }

  public void execute() throws MojoFailureException {
    Log log = getLog();
    log.info("Launching JUnit Platform...");

    if (skip) {
      log.info(MessageUtils.buffer().warning("JUnit Platform skipped.").toString());
      return;
    }

    Path mainPath = Paths.get(project.getBuild().getOutputDirectory());
    Path testPath = Paths.get(project.getBuild().getTestOutputDirectory());
    var world = new ModularWorld(mainPath, testPath);
    log.debug("");
    log.debug("Detected modular mode: " + world.getMode());
    log.debug("  main module: " + world.getMainModuleReference());
    log.debug("  test module: " + world.getTestModuleReference());

    int result = new JUnitPlatformConsoleStarter(this, world).getAsInt();
    if (result != 0) {
      throw new MojoFailureException("RED ALERT!");
    }
  }

  /** The entry point to Maven Artifact Resolver, i.e. the component doing all the work. */
  @Component private RepositorySystem repoSystem;

  /** The current repository/network configuration of Maven. */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  private RepositorySystemSession repoSession;

  /** The project's remote repositories to use for the resolution. */
  @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
  private List<RemoteRepository> remoteRepos;

  @Override
  public List<Artifact> loadArtifacts(String artifactCoords) throws Exception {
    Log log = getLog();

    Artifact artifact = new DefaultArtifact(artifactCoords);

    ArtifactRequest request = new ArtifactRequest();
    request.setArtifact(artifact);
    request.setRepositories(remoteRepos);

    log.info(String.format("Resolving artifact %s from %s", artifact, remoteRepos));

    ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);

    log.info(
        String.format(
            "Resolved artifact %s to %s from %s",
            artifact, result.getArtifact().getFile(), result.getRepository()));

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(remoteRepos);

    DependencyRequest dependencyRequest =
        new DependencyRequest(collectRequest, (all, ways) -> true);

    List<ArtifactResult> artifactResults =
        repoSystem.resolveDependencies(repoSession, dependencyRequest).getArtifactResults();

    for (ArtifactResult artifactResult : artifactResults) {
      log.info(
          String.format(
              "%s resolved to %s",
              artifactResult.getArtifact(), artifactResult.getArtifact().getFile()));
    }

    return artifactResults.stream().map(ArtifactResult::getArtifact).collect(Collectors.toList());
  }
}
