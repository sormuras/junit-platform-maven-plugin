package junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JUnit5Tests {

  @Test
  void test() {
    assertEquals(3, 1 + 2);
  }
}
