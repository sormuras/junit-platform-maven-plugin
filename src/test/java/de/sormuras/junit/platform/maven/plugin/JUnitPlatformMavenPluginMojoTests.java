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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JUnitPlatformMavenPluginMojoTests {

  @RegisterExtension MojoExtension extension = new MojoExtension();

  @Test
  void project99() throws Exception {
    Path base = Paths.get("target", "test-classes", "project", "99");
    assertTrue(Files.isDirectory(base));

    Mojo mojo = extension.lookupConfiguredMojo(base.toFile(), "launch-junit-platform");
    assertNotNull(mojo);
    assertTrue(mojo instanceof JUnitPlatformMavenPluginMojo);

    Configuration configuration = (Configuration) mojo;
    assertNotNull(configuration.getMavenProject());
    assertEquals(99L, configuration.getTimeout().getSeconds());
    assertEquals(Paths.get("reports", "99"), configuration.getReports());
    assertFalse(configuration.isStrict());
  }
}
