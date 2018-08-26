package application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class HelperTests {

  @Test
  void helper() {
    assertEquals("Helper", new Helper().getClass().getSimpleName());
  }
}
