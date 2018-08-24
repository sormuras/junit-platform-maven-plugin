package foo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class FooTests {

  @Test
  void accessClassFoo() {
    assertEquals("Foo", new Foo().getClass().getSimpleName(), "Simple name should be 'Foo'!");
  }

  @Test
  void accessModuleFoo() {
    assertEquals("foo", getClass().getModule().getName(), "Class does not reside in module 'foo'!");
  }
}
