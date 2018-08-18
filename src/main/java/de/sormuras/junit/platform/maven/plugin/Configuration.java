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

import java.time.Duration;
import java.util.Properties;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

interface Configuration {

  /** The underlying Maven project. */
  MavenProject getMavenProject();

  /** Log instance. */
  Log getLog();

  /** Global timeout duration in seconds. */
  Duration getTimeout();

  /**
   * Strict fail-fast mode switch.
   *
   * <h3>Console Launcher equivalents</h3>
   *
   * {@code --fail-if-no-tests}
   */
  boolean isStrict();

  /**
   * Path to directory for storing reports, like test result files.
   *
   * <p>The directory will be created if it does not exist. A relative path is resolved below the
   * current build directory, normally {@code target}. An empty path disables the generation of
   * reports.
   *
   * <h3>Console Launcher equivalent</h3>
   *
   * {@code --reports-dir}
   */
  String getReports();

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
  Properties getParameters();
}
