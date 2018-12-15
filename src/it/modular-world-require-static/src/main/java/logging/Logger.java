package logging;

public interface Logger {
  public void info(String message);

  public interface Factory {
    Logger getLogger(String category);
  }
}
