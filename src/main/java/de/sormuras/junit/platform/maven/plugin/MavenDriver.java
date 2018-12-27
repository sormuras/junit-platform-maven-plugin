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

import static de.sormuras.junit.platform.isolator.GroupArtifact.ISOLATOR_WORKER;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
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

    MavenProject project = mojo.getMavenProject();
    Tweaks tweaks = mojo.getTweaks();

    Set<Path> mainPaths = new LinkedHashSet<>();
    Set<Path> testPaths = new LinkedHashSet<>();
    Set<Path> launcherPaths = new LinkedHashSet<>();
    Set<Path> isolatorPaths = new LinkedHashSet<>();

    // Acquire all main and test path elements from the project...
    try {
      addAll(project.getCompileClasspathElements(), mainPaths);

      // Exclude all compile elements from test paths per default
      Set<String> excludePaths = new LinkedHashSet<>(project.getCompileClasspathElements());
      if (tweaks.moveTestEnginesToLauncherClassLoader) {
        locate("org.junit.jupiter:junit-jupiter").ifPresent(excludePaths::add);
        locate(JUNIT_JUPITER_ENGINE).ifPresent(excludePaths::add);
        locate("org.junit.platform:junit-platform-engine").ifPresent(excludePaths::add);
      }
      addAll(project.getTestClasspathElements(), excludePaths, testPaths);
    } catch (DependencyResolutionRequiredException e) {
      throw new RuntimeException("Resolution required!", e);
    }

    // Add additional path elements...
    addAll(tweaks.additionalTestPathElements, testPaths);
    addAll(tweaks.additionalLauncherPathElements, launcherPaths);

    // Resolve additional and missing dependencies...
    try {
      // Tweaks first...
      for (String coordinates : tweaks.additionalTestDependencies) {
        testPaths.addAll(resolve(coordinates));
      }
      for (String coordinates : tweaks.additionalLauncherDependencies) {
        launcherPaths.addAll(resolve(coordinates));
      }

      // JUnit Platform Launcher, Console, and well-known TestEngine implementations
      if (missing(JUNIT_PLATFORM_LAUNCHER)) {
        launcherPaths.addAll(resolve(JUNIT_PLATFORM_LAUNCHER));
      }
      if (mojo.getExecutor().isInjectConsole() && missing(JUNIT_PLATFORM_CONSOLE)) {
        launcherPaths.addAll(resolve(JUNIT_PLATFORM_CONSOLE));
      }
      if (contains(JUNIT_JUPITER_API) && missing(JUNIT_JUPITER_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_JUPITER_ENGINE));
      }
      if (contains(JUNIT_JUPITER_API) && tweaks.moveTestEnginesToLauncherClassLoader) {
        launcherPaths.addAll(resolve(JUNIT_JUPITER_ENGINE));
      }
      if (contains("junit:junit") && missing(JUNIT_VINTAGE_ENGINE)) {
        launcherPaths.addAll(resolve(JUNIT_VINTAGE_ENGINE));
      }
      // Isolator + Worker
      if (mojo.getExecutor().isInjectWorker() && missing(ISOLATOR_WORKER)) {
        isolatorPaths.addAll(resolve(configuration.basic().getWorkerCoordinates()));
      }
    } catch (RepositoryException e) {
      throw new RuntimeException("Resolution failed!", e);
    }

    mojo.removeExcludedArtifacts(mainPaths, testPaths, launcherPaths, isolatorPaths);

    Isolation isolation = mojo.getIsolation();
    switch (isolation) {
      case ALMOST:
        Path mainClasses = Paths.get(project.getBuild().getOutputDirectory());
        mainPaths.remove(mainClasses);
        testPaths.add(mainClasses);
        // fall-through!
      case ABSOLUTE:
        put(paths, "main", mainPaths);
        put(paths, "test", testPaths);
        put(paths, "launcher", launcherPaths);
        put(paths, "isolator", isolatorPaths);
        break;
      case MERGED:
        Set<Path> mergedPaths = new LinkedHashSet<>();
        mergedPaths.addAll(testPaths);
        mergedPaths.addAll(mainPaths);
        put(paths, "merged(test+main)", mergedPaths);
        put(paths, "launcher", launcherPaths);
        put(paths, "isolator", isolatorPaths);
        break;
      case NONE:
        Set<Path> allPaths = new LinkedHashSet<>();
        allPaths.addAll(mainPaths);
        allPaths.addAll(testPaths);
        allPaths.addAll(launcherPaths);
        allPaths.addAll(isolatorPaths);
        paths.put("all", allPaths);
        break;
      default:
        throw new AssertionError("Unsupported isolation constant: " + isolation);
    }

    pruneDuplicates(paths);

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

  private Optional<String> locate(GroupArtifact groupArtifact) {
    return locate(groupArtifact.toString());
  }

  private Optional<String> locate(String groupArtifact) {
    org.apache.maven.artifact.Artifact artifact =
        mojo.getMavenProject().getArtifactMap().get(groupArtifact);
    if (artifact == null) {
      return Optional.empty();
    }
    return Optional.of(artifact.getFile().toPath().toString());
  }

  private Set<Path> resolve(GroupArtifact groupArtifact) throws RepositoryException {
    return resolve(groupArtifact.toString(mojo::version));
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
    // debug("Resolving dependencies {0}...", dependencyRequest);
    return repositorySystem
        .resolveDependencies(session, dependencyRequest)
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        // .peek(a -> debug("Artifact {0} resolved to {1}", a, a.getFile()))
        .collect(Collectors.toList());
  }

  private static void addAll(Collection<String> source, Collection<Path> target) {
    addAll(source, Collections.emptySet(), target);
  }

  private static void addAll(
      Collection<String> source, Set<String> exclude, Collection<Path> target) {
    source
        .stream()
        .peek(System.out::println)
        .filter(a -> !exclude.contains(a))
        .map(Paths::get)
        .forEach(target::add);
  }

  private static void put(Map<String, Set<Path>> paths, String key, Set<Path> value) {
    if (value.isEmpty()) {
      return;
    }
    paths.put(key, value);
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
