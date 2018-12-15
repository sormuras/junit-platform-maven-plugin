import org.junit.jupiter.api.Test;

class TimeoutTests {

  @Test
  void hibernate() throws Exception {
    Thread.sleep(60 * 1000);
  }
}
