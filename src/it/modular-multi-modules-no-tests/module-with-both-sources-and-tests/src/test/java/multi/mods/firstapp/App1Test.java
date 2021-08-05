package multi.mods.firstapp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class App1Test {
  @Test
  void test_app1_returning_true() {
    assertTrue(new App1().returnTrue());
  }
}
