package logging;

public class SLF4JLoggerFactory implements Logger.Factory {
  @Override
  public Logger getLogger(String category) {
    return new SLF4JLogger(category);
  }

  private static class SLF4JLogger implements Logger {
    private final org.slf4j.Logger logger;

    private SLF4JLogger(String category) {
      logger = org.slf4j.LoggerFactory.getLogger(category);
    }

    @Override
    public void info(String message) {
      logger.info(message);
    }
  }
}
