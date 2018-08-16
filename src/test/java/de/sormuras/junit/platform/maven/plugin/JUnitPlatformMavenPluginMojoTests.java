package de.sormuras.junit.platform.maven.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.apache.maven.plugin.testing.MojoExtension;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JUnitPlatformMavenPluginMojoTests {

  @RegisterExtension MojoExtension mojo = new MojoExtension();

  /** @throws Exception if any */
  @Test
  void testSomething() throws Exception {
    File pom = new File("target/test-classes/project-to-test/");
    assertNotNull(pom);
    assertTrue(pom.exists());

    JUnitPlatformMavenPluginMojo platformMavenPluginMojo =
        (JUnitPlatformMavenPluginMojo) mojo.lookupConfiguredMojo(pom, "launch-junit-platform");
    assertNotNull(platformMavenPluginMojo);

    // platformMavenPluginMojo.execute();
  }

  /** Do not need the MojoRule. */
  @WithoutMojo
  @Test
  void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
    assertTrue(true);
  }
}
