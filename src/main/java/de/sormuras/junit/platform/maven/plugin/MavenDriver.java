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
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_PLATFORM_CONSOLE;
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_PLATFORM_LAUNCHER;
import static de.sormuras.junit.platform.isolator.GroupArtifact.JUNIT_VINTAGE_ENGINE;

import de.sormuras.junit.platform.isolator.Configuration;
import de.sormuras.junit.platform.isolator.Driver;
import de.sormuras.junit.platform.isolator.GroupArtifact;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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

  MavenDriver(JUnitPlatformMojo mojo, Configuration configuration) {
    this.mojo = mojo;
    this.configuration = configuration;
    this.repositories = new ArrayList<>();
    repositories.addAll(mojo.getMavenProject().getRemotePluginRepositories());
    repositories.addAll(mojo.getMavenProject().getRemoteProjectRepositories());
    this.repositorySystem = mojo.getMavenResolver();
    this.session = mojo.getMavenRepositorySession();
    this.paths = new LinkedHashMap<>();
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

  @Override
  public void error(String format, Object... objects) {
    mojo.error(format, objects);
  }

  @Override
  public Map<String, Set<Path>> paths() {
    if (!paths.isEmpty()) {
      return paths;
    }
    Set<Path> mainPaths = new LinkedHashSet<>();
    Set<Path> testPaths = new LinkedHashSet<>();
    Set<Path> launcherPaths = new LinkedHashSet<>();
    Set<Path> isolatorPaths = new LinkedHashSet<>();

    try {
      //
      // Main path elements
      //
      mojo.getMavenProject()
          .getCompileClasspathElements()
          .stream()
          .map(Paths::get)
          .forEach(mainPaths::add);

      //
      // Test path elements
      //
      mojo.getMavenProject()
          .getTestClasspathElements()
          .stream()
          .map(Paths::get)
          .forEach(testPaths::add);
    } catch (DependencyResolutionRequiredException e) {
      throw new RuntimeException("Resolution required!", e);
    }

    try {
      //
      // JUnit Platform Launcher, Console, and all TestEngine implementations
      //
      if (missing(JUNIT_PLATFORM_LAUNCHER)) {
        launcherPaths.addAll(resolve(JUNIT_PLATFORM_LAUNCHER.toString(mojo::version)));
      }
      if (mojo.getExecutor() != Executor.DIRECT && missing(JUNIT_PLATFORM_CONSOLE)) {
        launcherPaths.addAll(resolve(JUNIT_PLATFORM_CONSOLE.toString(mojo::version)));
      }
      if (contains(JUNIT_JUPITER_API) && missing(JUNIT_JUPITER_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_JUPITER_ENGINE.toString(mojo::version)));
      }
      if (contains("junit:junit") && missing(JUNIT_VINTAGE_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_VINTAGE_ENGINE.toString(mojo::version)));
      }
      //
      // Isolator + Worker
      //
      isolatorPaths.addAll(resolve(configuration.basic().getWorkerCoordinates()));
    } catch (RepositoryException e) {
      throw new RuntimeException("Resolution failed!", e);
    }

    // Classes in main output directory need a special treatment for now
    if (mojo.isReunite()) {
      Path mainClasses = Paths.get(mojo.getMavenProject().getBuild().getOutputDirectory());
      if (!mainPaths.remove(mainClasses)) {
        warn(
            "Main compile target output directory not part of projects compile classpath elements: {0}",
            mainClasses);
      }
      testPaths.add(mainClasses);
    }

    //
    // Only map non-empty path sets and remove duplicates
    //
    if (!mainPaths.isEmpty()) paths.put("main", mainPaths);
    if (!testPaths.isEmpty()) paths.put("test", testPaths);
    if (!launcherPaths.isEmpty()) paths.put("launcher", launcherPaths);
    paths.put("isolator", isolatorPaths);
    pruneDuplicates(paths);

    //
    // Throw all path elements into a single "all" set?
    //
    if (!mojo.isIsolate()) {
      Set<Path> allPaths = new LinkedHashSet<>();
      paths.values().forEach(allPaths::addAll);
      paths.clear();
      paths.put("all", allPaths);
    }

    return paths;
  }

  private boolean missing(GroupArtifact groupArtifact) {
    return !contains(groupArtifact);
  }

  private boolean contains(GroupArtifact groupArtifact) {
    return contains(groupArtifact.toString());
  }

  private boolean contains(String groupArtifact) {
    return mojo.getMavenProject().getArtifactMap().containsKey(groupArtifact);
  }

  private Set<Path> resolve(String coordinates) throws RepositoryException {
    return resolve(coordinates, "", (all, ways) -> true)
        .stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .collect(Collectors.toCollection(LinkedHashSet::new));
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

  static <T> void pruneDuplicates(Map<String, ? extends Collection<T>> paths) {
    Set<String> keys = paths.keySet();
    for (String outer : keys) {
      for (String inner : keys) {
        if (!outer.equals(inner)) {
          paths.get(inner).removeAll(paths.get(outer));
        }
      }
    }
  }
}
