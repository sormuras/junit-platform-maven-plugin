import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FailureTests {

  @Test
  void fail() throws Exception {
    Assertions.fail("on purpose");
  }
}
