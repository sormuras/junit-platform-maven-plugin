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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;

class Resolver {

  private final JUnitPlatformMojo mojo;
  private final List<RemoteRepository> repositories;

  Resolver(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.repositories = new ArrayList<>();
    repositories.addAll(mojo.getMavenProject().getRemotePluginRepositories());
    repositories.addAll(mojo.getMavenProject().getRemoteProjectRepositories());
  }

  List<Artifact> resolve(String coordinates, String scope) throws DependencyResolutionException {
    DefaultArtifact artifact = new DefaultArtifact(coordinates);
    mojo.debug("Resolving artifact %s from %s...", artifact, repositories);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, scope));
    collectRequest.setRepositories(repositories);
    DependencyRequest dependencyRequest =
        new DependencyRequest(collectRequest, (all, ways) -> true);
    RepositorySystemSession session = mojo.getMavenRepositorySession();
    mojo.debug("Resolving dependencies %s...", dependencyRequest);
    return mojo.getMavenResolver()
        .resolveDependencies(session, dependencyRequest)
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        .peek(a -> mojo.debug("Artifact %s resolved to %s", a, a.getFile()))
        .collect(Collectors.toList());
  }

  Artifact resolve(String coordinates) throws ArtifactResolutionException {
    DefaultArtifact artifact = new DefaultArtifact(coordinates);
    mojo.debug("Resolving artifact %s from %s...", artifact, repositories);
    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(repositories);
    ArtifactResult resolved =
        mojo.getMavenResolver().resolveArtifact(mojo.getMavenRepositorySession(), artifactRequest);
    mojo.debug("Resolved %s from %s", artifact, resolved.getRepository());
    mojo.debug("Stored %s to %s", artifact, resolved.getArtifact().getFile());
    return resolved.getArtifact();
  }
}
