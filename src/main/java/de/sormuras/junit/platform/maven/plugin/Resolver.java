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

import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_JUPITER_API;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_JUPITER_ENGINE;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_PLATFORM_CONSOLE;
import static de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact.JUNIT_VINTAGE_ENGINE;

import de.sormuras.junit.platform.maven.plugin.Dependencies.GroupArtifact;
import java.nio.file.Files;
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

  class PathsBuilder {

    private final List<Path> elements = new ArrayList<>();
    private final MavenProject project = mojo.getMavenProject();

    private void append(Path path) {
      if (Files.notExists(path)) {
        // debug("  X %s // does not exist", path);
        return;
      }
      if (elements.contains(path)) {
        // debug("  X %s // already added", path);
        return;
      }
      // debug(" -> %s", path);
      elements.add(path);
    }

    private void append(String first, String... more) {
      append(Paths.get(first, more));
    }

    List<Path> build() {
      append(project.getBuild().getTestOutputDirectory());
      append(project.getBuild().getOutputDirectory());

      // deps from pom
      for (var artifact : project.getArtifacts()) {
        if (!artifact.getArtifactHandler().isAddedToClasspath()) {
          continue;
        }
        var file = artifact.getFile();
        if (file != null) {
          append(file.toPath());
        }
      }

      // deps from here (auto-complete)
      try {
        // junit-jupiter-engine
        if (contains(JUNIT_JUPITER_API)) {
          resolve(JUNIT_JUPITER_ENGINE);
        }
        // junit-vintage-engine
        var junit = project.getArtifactMap().get("junit:junit");
        if (junit != null && "4.12".equals(junit.getVersion())) {
          resolve(JUNIT_VINTAGE_ENGINE);
        }
        // junit-platform-console
        resolve(JUNIT_PLATFORM_CONSOLE);
      } catch (DependencyResolutionException e) {
        throw new IllegalStateException("Resolving path elements failed", e);
      }

      return List.copyOf(elements);
    }

    private boolean contains(Dependencies.GroupArtifact ga) {
      return project.getArtifactMap().containsKey(ga.toIdentifier());
    }

    private void resolve(GroupArtifact ga) throws DependencyResolutionException {
      if (contains(ga)) {
        // debug("Skip resolving '%s', because it is already mapped.", ga);
        return;
      }
      var gav = ga.toIdentifier() + ":" + mojo.version(ga.getVersion());
      // debug("");
      // debug("Resolving '%s' and its transitive dependencies...", gav);
      for (var resolved : resolve(gav)) {
        var key = resolved.getGroupId() + ':' + resolved.getArtifactId();
        if (project.getArtifactMap().containsKey(key)) {
          // debug("  X %s // mapped by project", resolved);
          continue;
        }
        append(resolved.getFile().toPath().toAbsolutePath().normalize());
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

  private final JUnitPlatformMojo mojo;
  private final List<Path> paths;

  Resolver(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.paths = new PathsBuilder().build();
  }

  List<Path> getPaths() {
    return paths;
  }
}
