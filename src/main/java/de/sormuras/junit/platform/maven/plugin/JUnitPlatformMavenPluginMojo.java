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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;

/** Launch JUnit Platform Mojo. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
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
}
