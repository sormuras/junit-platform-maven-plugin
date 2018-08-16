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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.apache.maven.plugin.testing.MojoExtension;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JUnitPlatformMavenPluginMojoTests {

  @RegisterExtension MojoExtension mojo = new MojoExtension();

  @Test
  void testSomething() throws Exception {
    File pom = new File("target/test-classes/project-to-test/");
    assertNotNull(pom);
    assertTrue(pom.exists());

    JUnitPlatformMavenPluginMojo platformMavenPluginMojo =
        (JUnitPlatformMavenPluginMojo) mojo.lookupConfiguredMojo(pom, "launch-junit-platform");
    assertNotNull(platformMavenPluginMojo);
    assertNotNull(platformMavenPluginMojo.getProject());
  }

  @WithoutMojo
  @Test
  void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
    assertTrue(true);
  }
}
