package logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Assert;
import org.junit.Test;

public class LoggingTest {
  @Test
  public void test() throws Exception {
    PrintStream original = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer));
    try {
      Logger.Factory factory = new SLF4JLoggerFactory();
      Logger logger = factory.getLogger("foo");
      String log = "hello world";
      logger.info(log);

      String text = buffer.toString("ASCII");
      Assert.assertFalse(text.startsWith(log));
    } finally {
      System.setErr(original);
    }
  }
}
