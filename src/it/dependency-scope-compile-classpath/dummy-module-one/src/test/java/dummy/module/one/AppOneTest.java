package dummymodule.module.one;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppOneTest {
  @Test
  void test_app_one_returns_true() {
    assertTrue(new AppOne().returnTrue());
  }
}
