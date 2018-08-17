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

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class Task implements IntSupplier {

  private final ClassLoader classLoader;
  private final Configuration configuration;

  public Task(ClassLoader classLoader, Configuration configuration) {
    this.classLoader = classLoader;
    this.configuration = configuration;
  }

  @Override
  public int getAsInt() {
    ClassLoader oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return launchJUnitPlatform(configuration);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private static int launchJUnitPlatform(Configuration configuration) {
    Log log = configuration.getLog();
    log.info("Launching JUnit Platform...");
    log.info("");
    log.debug("project: " + configuration.getMavenProject());
    log.debug("timeout: " + configuration.getTimeout().getSeconds());
    log.debug("");

    Set<Path> roots = new HashSet<>();
    roots.add(Paths.get(configuration.getMavenProject().getBuild().getTestOutputDirectory()));

    LauncherDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request().selectors(selectClasspathRoots(roots)).build();

    Launcher launcher = LauncherFactory.create();

    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(listener);

    long startTimeMillis = System.currentTimeMillis();
    launcher.execute(request);
    long duration = System.currentTimeMillis() - startTimeMillis;

    TestExecutionSummary summary = listener.getSummary();

    boolean success =
        summary.getTestsFailedCount() == 0
            && summary.getTestsAbortedCount() == 0
            && summary.getContainersFailedCount() == 0
            && summary.getContainersAbortedCount() == 0;

    if (success) {
      long succeeded = summary.getTestsSucceededCount();
      log.info(String.format("Successfully executed: %d test(s) in %d ms", succeeded, duration));
    } else {
      StringWriter message = new StringWriter();
      PrintWriter writer = new PrintWriter(message);
      summary.printTo(writer);
      summary.printFailuresTo(writer);
      for (String line : message.toString().split("\\R")) {
        log.error(line);
      }
    }

    return success ? 0 : 1;
  }
}
