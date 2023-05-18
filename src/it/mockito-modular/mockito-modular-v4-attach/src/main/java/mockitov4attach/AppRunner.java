package mockitov4attach;

public final class AppRunner {
  private final AppInterface runningApp;

  public AppRunner(final AppInterface appToRun) {
    runningApp = appToRun;
  }

  public boolean isAppRunning() {
    return runningApp.running();
  }
}
