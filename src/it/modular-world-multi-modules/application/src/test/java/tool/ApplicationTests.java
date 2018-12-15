package application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ApplicationTests {

  @Test
  void application() {
    assertEquals("Application", new Application().getClass().getSimpleName());
  }
}
