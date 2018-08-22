package foo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class FooTests {

  @Test
  void test() {
    assertEquals("foo", getClass().getModule().getName(), "Class does not reside in module 'foo'!");
  }
}
