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
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.logging.MessageUtils;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractBaseMojo {

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

  /**
   * Global timeout duration in seconds.
   *
   * @return timeout duration in seconds
   */
  Duration getTimeout() {
    return Duration.ofSeconds(timeout);
  }

  /**
   * Strict fail-fast mode switch.
   *
   * <h3>Console Launcher equivalents</h3>
   *
   * {@code --fail-if-no-tests}
   *
   * @return {@code true} to switch on all checks
   */
  boolean isStrict() {
    return strict;
  }

  /**
   * Directory for storing reports, like test result files.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --reports-dir <String>}
   *
   * @return path to reports directory, may be empty
   * @see #getReportsPath()
   */
  String getReports() {
    return reports;
  }

  /**
   * Path to directory for storing reports, like test result files.
   *
   * <p>The directory will be created if it does not exist. A relative path is resolved below the
   * current build directory, normally {@code target}. An empty path disables the generation of
   * reports.
   *
   * @return path to reports directory, may be empty
   * @see #getReports()
   */
  Optional<Path> getReportsPath() {
    var reports = getReports();
    if (reports.trim().isEmpty()) { // .isBlank()
      return Optional.empty();
    }
    Path path = Paths.get(reports);
    if (path.isAbsolute()) {
      return Optional.of(path);
    }
    return Optional.of(Paths.get(getMavenProject().getBuild().getDirectory()).resolve(path));
  }

  /**
   * Launcher configuration parameters.
   *
   * <p>Set a configuration parameter for test discovery and execution.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --config <key=value>}
   *
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params">Configuration
   *     Parameters</a>
   */
  Map<String, String> getParameters() {
    return parameters;
  }

  /**
   * Tags or tag expressions to include only tests whose tags match.
   *
   * <p>All tags and expressions will be combined using {@code OR} semantics.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --include-tag <String>}
   *
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions">Tag
   *     Expressions</a>
   * @see <a
   *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering">Tagging
   *     and Filtering</a>
   */
  List<String> getTags() {
    return tags;
  }

  public void execute() throws MojoFailureException {
    Log log = getLog();
    log.info("Launching JUnit Platform...");

    if (skip) {
      log.info(MessageUtils.buffer().warning("JUnit Platform execution skipped.").toString());
      return;
    }

    var build = getMavenProject().getBuild();
    Path mainPath = Paths.get(build.getOutputDirectory());
    Path testPath = Paths.get(build.getTestOutputDirectory());
    var modules = new Modules(mainPath, testPath);
    log.debug("");
    log.debug("Main module reference -> " + modules.getMainModuleReference());
    log.debug("Test module reference -> " + modules.getTestModuleReference());

    int result = new JUnitPlatformStarter(this, modules).getAsInt();
    if (result != 0) {
      throw new MojoFailureException("RED ALERT!");
    }
  }
}
