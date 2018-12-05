package de.sormuras.junit.platform.maven.plugin;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Creates, configures and starts the JUnit Platform via its Launcher API. */
class SeparatingLauncher implements Callable<TestExecutionSummary> {

  private final JUnitPlatformMojo mojo;
  private final Log log;
  private final Build build;
  private final LauncherDiscoveryRequest request;

  SeparatingLauncher(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
    this.log = mojo.getLog();
    this.build = mojo.getMavenProject().getBuild();
    this.request = buildRequest();

    log.debug("");
    log.debug("Created JUnit LauncherDiscoveryRequest" + request);
    log.debug("  discovery selectors: " + request.getSelectorsByType(DiscoverySelector.class));
    log.debug("  discovery filters: " + request.getFiltersByType(DiscoveryFilter.class));
    log.debug("  engine filters: " + request.getEngineFilters());
    log.debug("  post test descriptor filters: " + request.getPostDiscoveryFilters());
  }

  private LauncherDiscoveryRequest buildRequest() {
    LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
    // selectors
    Set<Path> roots = new HashSet<>();
    roots.add(Paths.get(build.getTestOutputDirectory()));
    builder.selectors(selectClasspathRoots(roots));
    // filters
    if (!mojo.getTags().isEmpty()) {
      builder.filters(TagFilter.includeTags(mojo.getTags()));
    }
    // parameters
    builder.configurationParameters(mojo.getParameters());
    return builder.build();
  }

  @Override
  public TestExecutionSummary call() throws ReflectiveOperationException {
    log.info("Launching JUnit Platform...");

    String name = "de.sormuras.junit.platform.manager.Manager";
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    Launcher launcher = (Launcher) Class.forName(name, true, loader).getConstructor().newInstance();

    discover(launcher);
    if (mojo.isDryRun()) {
      return new ZeroSummary();
    }
    return execute(launcher);
  }

  private void discover(Launcher launcher) {
    log.debug("Discovering tests...");

    TestPlan testPlan = launcher.discover(request);
    testPlan.getRoots().forEach(engine -> log.info(" o " + engine.getDisplayName()));
    log.info("");

    if (!testPlan.containsTests()) {
      String message = "Zero tests discovered!";
      log.warn(message);
      throw new AssertionError(message);
    }
  }

  private TestExecutionSummary execute(Launcher launcher) {
    log.debug("Executing tests...");

    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(summary);

    // TODO https://github.com/junit-team/junit5/issues/1375
    //    String reports = configuration.getReports();
    //    if (!reports.trim().isEmpty()) {
    //      Path path = Paths.get(reports);
    //      if (!path.isAbsolute()) {
    //        path = Paths.get(build.getDirectory()).resolve(path);
    //      }
    //      launcher.register...(new XmlReportsWritingListener(path, log::error));
    //    }

    launcher.execute(request);

    return summary.getSummary();
  }

  static class ZeroSummary implements TestExecutionSummary {

    @Override
    public long getTimeStarted() {
      return 0;
    }

    @Override
    public long getTimeFinished() {
      return 0;
    }

    @Override
    public long getTotalFailureCount() {
      return 0;
    }

    @Override
    public long getContainersFoundCount() {
      return 0;
    }

    @Override
    public long getContainersStartedCount() {
      return 0;
    }

    @Override
    public long getContainersSkippedCount() {
      return 0;
    }

    @Override
    public long getContainersAbortedCount() {
      return 0;
    }

    @Override
    public long getContainersSucceededCount() {
      return 0;
    }

    @Override
    public long getContainersFailedCount() {
      return 0;
    }

    @Override
    public long getTestsFoundCount() {
      return 0;
    }

    @Override
    public long getTestsStartedCount() {
      return 0;
    }

    @Override
    public long getTestsSkippedCount() {
      return 0;
    }

    @Override
    public long getTestsAbortedCount() {
      return 0;
    }

    @Override
    public long getTestsSucceededCount() {
      return 0;
    }

    @Override
    public long getTestsFailedCount() {
      return 0;
    }

    @Override
    public void printTo(PrintWriter printWriter) {
      printWriter.println("No summary available.");
    }

    @Override
    public void printFailuresTo(PrintWriter printWriter) {
      printWriter.println("No failures available.");
    }

    @Override
    public List<Failure> getFailures() {
      return List.of();
    }
  }
}
