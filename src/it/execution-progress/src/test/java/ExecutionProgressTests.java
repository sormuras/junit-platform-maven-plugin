import org.junit.jupiter.api.Test;

class ExecutionProgressTests {

  @Test
  void count() throws Exception {
    for (int i = 0; i < 300; i++) {
      Thread.sleep(200);
      System.out.println(i);
    }
  }
}
