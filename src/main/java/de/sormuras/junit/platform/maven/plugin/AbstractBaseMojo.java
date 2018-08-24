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

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;

/** Provides basic utility helpers. */
abstract class AbstractBaseMojo extends AbstractMojo {

  /** Detected versions extracted from the project's dependencies. */
  private Map<String, String> detectedVersions;

  /** Module system helper. */
  private Modules modules;

  /** The project. */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /** The project's remote repositories to use for the resolution. */
  @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
  private List<RemoteRepository> repositories;

  /** The entry point to Maven Artifact Resolver, i.e. the component doing all the work. */
  @Component private RepositorySystem resolver;

  /** The current repository/network configuration of Maven. */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
  private RepositorySystemSession session;

  private Modules createModules(Build build) {
    var mainPath = Paths.get(build.getOutputDirectory());
    var testPath = Paths.get(build.getTestOutputDirectory());
    return new Modules(mainPath, testPath);
  }

  private Map<String, String> createAutomaticDetectedVersions(MavenProject project) {
    var map = project.getArtifactMap();

    String jupiterVersion;
    var jupiterEngine = map.get("org.junit.jupiter:junit-jupiter-engine");
    if (jupiterEngine != null) {
      jupiterVersion = jupiterEngine.getVersion();
    } else {
      var jupiterApi = map.get("org.junit.jupiter:junit-jupiter-api");
      if (jupiterApi != null) {
        jupiterVersion = jupiterApi.getVersion();
      } else {
        jupiterVersion = "5.3.0-RC1";
      }
    }

    String vintageVersion;
    var vintageEngine = map.get("org.junit.vintage:junit-vintage-engine");
    if (vintageEngine != null) {
      vintageVersion = vintageEngine.getVersion();
    } else {
      vintageVersion = "5.3.0-RC1";
    }

    String platformVersion;
    var platformCommons = map.get("org.junit.platform:junit-platform-commons");
    if (platformCommons != null) {
      platformVersion = platformCommons.getVersion();
    } else {
      platformVersion = "1.3.0-RC1";
    }

    return Map.of(
        "junit.jupiter.version", jupiterVersion,
        "junit.vintage.version", vintageVersion,
        "junit.platform.version", platformVersion);
  }

  void debug(String format, Object... args) {
    getLog().debug(String.format(format, args));
  }

  String getDetectedVersion(String key) {
    return detectedVersions.get(key);
  }

  MavenProject getMavenProject() {
    return project;
  }

  Modules getModules() {
    return modules;
  }

  void initialize() {
    modules = createModules(getMavenProject().getBuild());
    detectedVersions = createAutomaticDetectedVersions(getMavenProject());
  }

  List<Artifact> resolve(String coordinates) throws Exception {
    var artifact = new DefaultArtifact(coordinates);
    // debug("Resolving artifact %s from %s...", artifact, repositories);

    var artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(repositories);
    // var resolved = resolver.resolveArtifact(session, artifactRequest);
    // debug("Resolved %s from %s", artifact, resolved.getRepository());
    // debug("Stored %s to %s", artifact, resolved.getArtifact().getFile());

    var collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(repositories);

    var dependencyRequest = new DependencyRequest(collectRequest, (all, ways) -> true);
    // debug("Resolving dependencies %s...", dependencyRequest);
    var artifacts = resolver.resolveDependencies(session, dependencyRequest).getArtifactResults();

    return artifacts
        .stream()
        .map(ArtifactResult::getArtifact)
        // .peek(a -> debug("Artifact %s resolved to %s", a, a.getFile()))
        .collect(Collectors.toList());
  }
}
