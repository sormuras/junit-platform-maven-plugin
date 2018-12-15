import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class JupiterTests {

  @Test
  @Tag("bar")
  void test() throws Exception {
    Thread.sleep(200);
  }

  @Test
  @Tag("fail")
  void fail() throws Exception {
    throw new AssertionError();
  }
}
