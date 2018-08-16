package de.sormuras.junit.platform.maven.plugin;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Goal which touches a timestamp file. */
@Mojo(
    name = "launch-junit-platform",
    defaultPhase = LifecyclePhase.TEST,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.TEST)
public class JUnitPlatformMavenPluginMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    Log log = getLog();
    log.info("Launching JUnit Platform...");
    log.info("");
    log.info("project: " + project);
    log.info("   main: " + project.getBuild().getOutputDirectory());
    log.info("   test: " + project.getBuild().getTestOutputDirectory());
    try {
      log.debug("");
      log.debug("test class-path elements:");
      project.getTestClasspathElements().forEach(log::debug);
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Resolving test class-path elements failed", e);
    }
    log.info("");

    Set<Path> roots = new HashSet<>();
    roots.add(Paths.get(project.getBuild().getTestOutputDirectory()));

    LauncherDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClasspathRoots(roots))
            .filters(includeClassNamePatterns(".*Tests"))
            .build();

    Launcher launcher = LauncherFactory.create();

    // Register a listener of your choice
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(listener);

    launcher.execute(request);

    TestExecutionSummary summary = listener.getSummary();
    StringWriter message = new StringWriter();
    PrintWriter writer = new PrintWriter(message);
    summary.printTo(writer);
    summary.printFailuresTo(writer);

    log.info(message.toString());
  }
}
