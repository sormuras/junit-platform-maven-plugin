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

import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_JUPITER_API;
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_JUPITER_ENGINE;
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_PLATFORM_LAUNCHER;
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_VINTAGE_ENGINE;

import de.sormuras.junit.platform.isolator.Configuration;
import de.sormuras.junit.platform.isolator.Driver;
import de.sormuras.junit.platform.isolator.GroupArtifact;
import de.sormuras.junit.platform.isolator.Version;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;

class MavenDriver implements Driver {

  private final JUnitPlatformMojo mojo;
  private final Configuration configuration;
  private final Map<String, Set<Path>> paths;
  private final List<RemoteRepository> repositories;
  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession session;
  private final Map<String, String> versions;

  MavenDriver(JUnitPlatformMojo mojo, Configuration configuration) {
    this.mojo = mojo;
    this.configuration = configuration;
    this.repositories = new ArrayList<>();
    repositories.addAll(mojo.getMavenProject().getRemotePluginRepositories());
    repositories.addAll(mojo.getMavenProject().getRemoteProjectRepositories());
    this.repositorySystem = mojo.getMavenResolver();
    this.session = mojo.getMavenRepositorySession();
    this.paths = new LinkedHashMap<>();
    this.versions = Version.buildMap(this::artifactVersionOrNull);
  }

  private String artifactVersionOrNull(String key) {
    org.apache.maven.artifact.Artifact artifact = mojo.getMavenProject().getArtifactMap().get(key);
    if (artifact == null) {
      return null;
    }
    return artifact.getBaseVersion();
  }

  /** Lookup version as a {@link String}. */
  @Override
  public String version(Version version) {
    String detectedVersion = versions.get(version.getKey());
    return mojo.getVersions().getOrDefault(version.getKey(), detectedVersion);
  }

  @Override
  public void debug(String format, Object... objects) {
    mojo.debug(format, objects);
  }

  @Override
  public void info(String format, Object... objects) {
    mojo.info(format, objects);
  }

  @Override
  public void warn(String format, Object... objects) {
    mojo.warn(format, objects);
  }

  public boolean contains(GroupArtifact groupArtifact) {
    return contains(groupArtifact.toString());
  }

  public boolean contains(String groupArtifact) {
    return mojo.getMavenProject().getArtifactMap().containsKey(groupArtifact);
  }

  /** Resolve artifact and its transitive dependencies. */
  public Set<Path> resolve(String coordinates) throws Exception {
    return resolve(coordinates, "", (all, ways) -> true)
        .stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public Map<String, Set<Path>> paths() {
    if (!paths.isEmpty()) {
      return paths;
    }
    //
    // Main and Test
    //
    try {
      paths.put(
          "main",
          mojo.getMavenProject()
              .getCompileClasspathElements()
              .stream()
              .map(Paths::get)
              .collect(Collectors.toCollection(LinkedHashSet::new)));
      paths.put(
          "test",
          mojo.getMavenProject()
              .getTestClasspathElements()
              .stream()
              .map(Paths::get)
              .collect(Collectors.toCollection(LinkedHashSet::new)));
    } catch (DependencyResolutionRequiredException e) {
      throw new RuntimeException("Resolution required!", e);
    }

    //
    // JUnit Platform Launcher and all TestEngine implementations
    //
    try {
      Set<Path> launcherPaths = new LinkedHashSet<>();
      if (!contains(JUNIT_PLATFORM_LAUNCHER)) {
        launcherPaths.addAll(resolve(JUNIT_PLATFORM_LAUNCHER.toString(this::version)));
      }
      if (contains(JUNIT_JUPITER_API) && !contains(JUNIT_JUPITER_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_JUPITER_ENGINE.toString(this::version)));
      }
      if (contains("junit:junit") && !contains(JUNIT_VINTAGE_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_VINTAGE_ENGINE.toString(this::version)));
      }
      paths.put("launcher", launcherPaths);

      //
      // Worker + Manager
      //
      Set<Path> workerPaths = resolve(configuration.basic().getWorkerCoordinates());
      paths.put("worker", workerPaths);
    } catch (Exception e) {
      throw new RuntimeException("Resolution failed!", e);
    }

    //
    // Remove duplicates
    //
    paths.get("test").removeAll(paths.get("main"));
    paths.get("launcher").removeAll(paths.get("main"));
    paths.get("worker").removeAll(paths.get("main"));

    paths.get("launcher").removeAll(paths.get("test"));
    paths.get("worker").removeAll(paths.get("test"));

    paths.get("worker").removeAll(paths.get("launcher"));

    //
    // Throw all path elements into a single set?
    //
    if (!mojo.isIsolate()) {
      Set<Path> allPaths = new LinkedHashSet<>();
      paths.values().forEach(allPaths::addAll);
      paths.clear();
      paths.put("all", allPaths);
    }

    return paths;
  }

  private List<Artifact> resolve(String coordinates, String scope, DependencyFilter filter)
      throws RepositoryException {
    DefaultArtifact artifact = new DefaultArtifact(coordinates);
    debug("Resolving artifact {0} from {1}...", artifact, repositories);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, scope));
    collectRequest.setRepositories(repositories);
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
    debug("Resolving dependencies {0}...", dependencyRequest);
    return repositorySystem
        .resolveDependencies(session, dependencyRequest)
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        .peek(a -> debug("Artifact {0} resolved to {1}", a, a.getFile()))
        .collect(Collectors.toList());
  }
}
