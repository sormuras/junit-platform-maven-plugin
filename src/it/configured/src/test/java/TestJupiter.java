import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TestJupiter {

  @Test
  @Tag("foo")
  @Tag("bar")
  void test() throws Exception {
    Thread.sleep(200);
  }
}
