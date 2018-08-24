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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class JUnitPlatformMojoTests {

  // @RegisterExtension MojoExtension extension = new MojoExtension();

  @Test
  void project99() throws Exception {
    Path base = Paths.get("target", "test-classes", "project", "99");
    assertTrue(Files.isDirectory(base));

    //    Mojo configuredMojo = extension.lookupConfiguredMojo(base.toFile(),
    // "launch-junit-platform");
    //    assertNotNull(configuredMojo);
    //    assertTrue(configuredMojo instanceof JUnitPlatformMojo);

    //    JUnitPlatformMojo mojo = (JUnitPlatformMojo) configuredMojo;
    //    assertNotNull(mojo.getMavenProject());
    //    assertNotNull(mojo.getLog());
    //    assertEquals(99L, mojo.getTimeout().getSeconds());
    //    assertEquals(Paths.get("reports", "99"), Paths.get(mojo.getReports()));
    //    assertTrue(mojo.getReportsPath().orElseThrow().isAbsolute());
    //    assertTrue(mojo.getReportsPath().orElseThrow().endsWith(Paths.get("reports", "99")));
    //    assertAll(
    //        () -> assertEquals("!98", mojo.getTags().get(0)),
    //        () -> assertEquals("99", mojo.getTags().get(1)),
    //        () -> assertEquals("(a | b) & (c | !d)", mojo.getTags().get(2)),
    //        () -> assertEquals(3, mojo.getTags().size()));
    //    assertAll(
    //        () -> assertEquals("99", mojo.getParameters().get("ninety.nine")),
    //        () -> assertEquals(1, mojo.getParameters().size()));
    //    assertAll(
    //        () -> assertEquals("99-platform", mojo.getJUnitPlatformVersion()),
    //        () -> assertEquals("99-jupiter", mojo.getJUnitJupiterVersion()),
    //        () -> assertEquals("99-vintage", mojo.getJUnitVintageVersion()));
  }
}
