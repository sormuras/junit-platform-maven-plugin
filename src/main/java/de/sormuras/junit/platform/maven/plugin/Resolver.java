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

import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_JUPITER_ENGINE;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_PLATFORM_CONSOLE;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_VINTAGE_ENGINE;

import de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;

/** Runtime artifact resolver helper. */
class Resolver {

  private final JUnitPlatformMojo mojo;
  private final MavenProject project;
  private final List<Path> paths;

  Resolver(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.project = mojo.getMavenProject();
    this.paths = new ArrayList<>();

    initializePaths();
  }

  List<Path> getPaths() {
    return List.copyOf(paths);
  }

  private void initializePaths() {
    // test out
    paths.add(Paths.get(project.getBuild().getTestOutputDirectory()));
    // main out
    paths.add(Paths.get(project.getBuild().getOutputDirectory()));
    // deps from pom
    for (var artifact : project.getArtifacts()) {
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
      var map = project.getArtifactMap();
      // junit-jupiter-engine
      var jupiterApi = map.get("org.junit.jupiter:junit-jupiter-api");
      var jupiterEngine = "org.junit.jupiter:junit-jupiter-engine";
      if (jupiterApi != null && !map.containsKey(jupiterEngine)) {
        resolve(JUNIT_JUPITER_ENGINE);
      }
      // junit-vintage-engine
      var vintageApi = map.get("junit:junit");
      var vintageEngine = "org.junit.vintage:junit-vintage-engine";
      if (vintageApi != null && !map.containsKey(vintageEngine)) {
        if (vintageApi.getVersion().equals("4.12")) {
          resolve(JUNIT_VINTAGE_ENGINE);
        }
      }
      // junit-platform-console
      var platformConsole = "org.junit.platform:junit-platform-console";
      if (!map.containsKey(platformConsole)) {
        resolve(JUNIT_PLATFORM_CONSOLE);
      }
    } catch (DependencyResolutionException e) {
      throw new IllegalStateException("Resolving path elements failed", e);
    }
  }

  private void resolve(GroupArtifact grar) throws DependencyResolutionException {
    var map = project.getArtifactMap();
    var ga = grar.toIdentifier();
    if (map.containsKey(ga)) {
      mojo.debug("Skip resolving '%s', because it is already mapped.", grar);
      return;
    }
    var gav = ga + ":" + mojo.version(grar.getVersion());
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

  private List<Artifact> resolve(String coordinates) throws DependencyResolutionException {
    var repositories = project.getRemotePluginRepositories();
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
    var session = mojo.getMavenRepositorySession();
    // debug("Resolving dependencies %s...", dependencyRequest);
    return mojo.getMavenResolver()
        .resolveDependencies(session, dependencyRequest)
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        // .peek(a -> debug("Artifact %s resolved to %s", a, a.getFile()))
        .collect(Collectors.toList());
  }
}
