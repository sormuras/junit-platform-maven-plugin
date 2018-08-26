package foo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ReflectionWhiteBoxTests {

  @Test
  void reflectClassFoo() throws Exception {
    var loader = getClass().getClassLoader();
    assertDoesNotThrow(() -> loader.loadClass("foo.Foo"));
  }
}
