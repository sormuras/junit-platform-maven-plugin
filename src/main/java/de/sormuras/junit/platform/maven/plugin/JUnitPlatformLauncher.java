package de.sormuras.junit.platform.maven.plugin;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

import de.sormuras.junit.platform.maven.plugin.shadow.XmlReportsWritingListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Creates, configures and starts the JUnit Platform via its Launcher API. */
class JUnitPlatformLauncher implements Callable<TestExecutionSummary> {

  private final Configuration configuration;
  private final Log log;
  private final Build build;
  private final Launcher launcher;
  private final LauncherDiscoveryRequest request;

  JUnitPlatformLauncher(Configuration configuration) {
    this.configuration = configuration;
    this.log = configuration.getLog();
    this.build = configuration.getMavenProject().getBuild();
    this.launcher = LauncherFactory.create();
    this.request = buildRequest();
  }

  private LauncherDiscoveryRequest buildRequest() {
    Set<Path> roots = new HashSet<>();
    roots.add(Paths.get(build.getTestOutputDirectory()));

    LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectClasspathRoots(roots));
    if (!configuration.getTags().isEmpty()) {
      builder.filters(TagFilter.includeTags(configuration.getTags()));
    }
    builder.configurationParameters(configuration.getParameters());
    return builder.build();
  }

  @Override
  public TestExecutionSummary call() {
    log.info("Launching JUnit Platform...");
    discover();
    return execute();
  }

  private void discover() {
    log.debug("Discovering tests...");

    TestPlan testPlan = launcher.discover(request);
    testPlan.getRoots().forEach(engine -> log.info(" o " + engine.getDisplayName()));
    log.info("");

    if (!testPlan.containsTests()) {
      String message = "Zero tests discovered!";
      log.warn(message);
      if (configuration.isStrict()) {
        throw new AssertionError(message);
      }
    }
  }

  private TestExecutionSummary execute() {
    log.debug("Executing tests...");

    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(summary);

    String reports = configuration.getReports();
    if (!reports.trim().isEmpty()) {
      Path path = Paths.get(reports);
      if (!path.isAbsolute()) {
        path = Paths.get(build.getDirectory()).resolve(path);
      }
      launcher.registerTestExecutionListeners(new XmlReportsWritingListener(path, log::error));
    }

    launcher.execute(request);

    return summary.getSummary();
  }
}
