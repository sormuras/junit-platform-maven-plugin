package dummymodule.module.two;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppTwoTest {
  @Test
  void test_app_one_returns_true() {
    assertTrue(new AppTwo().returnTrue());
  }
}
