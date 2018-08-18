package de.sormuras.junit.platform.maven.plugin;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

import de.sormuras.junit.platform.maven.plugin.shadow.XmlReportsWritingListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

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

    Map<String, String> parameters = new HashMap<>();
    for (Map.Entry<Object, Object> entry : configuration.getParameters().entrySet()) {
      parameters.put((String) entry.getKey(), (String) entry.getValue());
    }

    return LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClasspathRoots(roots))
        .configurationParameters(parameters)
        .build();
  }

  @Override
  public TestExecutionSummary call() {
    log.info("Launching JUnit Platform...");
    log.info("");
    log.debug("project: " + configuration.getMavenProject());
    log.debug("timeout: " + configuration.getTimeout().getSeconds());
    log.debug("parameters: " + configuration.getParameters());
    log.debug("");
    discover();
    return execute();
  }

  private void discover() {
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
    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(summary);

    Path reports = configuration.getReports();
    if (!reports.isAbsolute()) {
      reports = Paths.get(build.getDirectory()).resolve(configuration.getReports());
    }
    launcher.registerTestExecutionListeners(new XmlReportsWritingListener(reports, log::error));

    launcher.execute(request);

    return summary.getSummary();
  }
}
