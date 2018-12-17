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

import de.sormuras.junit.platform.isolator.Modules;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.maven.project.MavenProject;

class JavaPatcher {

  private final JUnitPlatformMojo mojo;
  private final MavenProject project;
  private final Modules modules;

  JavaPatcher(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.project = mojo.getMavenProject();
    this.modules = mojo.getProjectModules();
  }

  void patch(List<String> cmd) {
    String testSource = project.getBuild().getTestSourceDirectory();
    String testOutput = project.getBuild().getTestOutputDirectory();
    String name = modules.getMainModuleName().orElseThrow(AssertionError::new);

    mojo.debug("");
    mojo.debug("Patching tests into main module {0} <- {1}", name, testOutput);
    cmd.add("--patch-module");
    cmd.add(name + '=' + testOutput);

    // Apply user-defined command line options defined in "module-info.test"
    Optional<Path> moduleInfoTest = findModuleInfoTest(testSource, testOutput);
    if (moduleInfoTest.isPresent()) {
      Path moduleInfoTestPath = moduleInfoTest.get();
      mojo.debug("Using lines of {0} to patch module {1}...", moduleInfoTestPath, name);
      appendModuleInfoTestArguments(moduleInfoTestPath, cmd::add);
    }
  }

  private Optional<Path> findModuleInfoTest(String... roots) {
    for (String root : roots) {
      Path candidate = Paths.get(root).resolve("module-info.test");
      if (Files.exists(candidate)) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  private static void appendModuleInfoTestArguments(
      Path moduleInfoTestPath, Consumer<String> consume) {
    try (Stream<String> lines = Files.lines(moduleInfoTestPath)) {
      lines
          .map(String::trim)
          .filter(line -> !line.isEmpty())
          .filter(line -> !line.startsWith("//"))
          .forEach(consume);
    } catch (IOException e) {
      throw new UncheckedIOException("Reading " + moduleInfoTestPath + " failed", e);
    }
  }
}
