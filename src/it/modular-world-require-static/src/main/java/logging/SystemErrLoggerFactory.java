package logging;

public class SystemErrLoggerFactory implements Logger.Factory {
  @Override
  public Logger getLogger(String category) {
    return new SystemErrLogger(category);
  }

  private static class SystemErrLogger implements Logger {
    public SystemErrLogger(String category) {}

    @Override
    public void info(String message) {
      System.err.println(message);
    }
  }
}
