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
    assertEquals("foo", getClass().getModule().getName(), "Class doesn't reside in module 'foo'!");
  }

  @Test
  void getMainResourceViaClassLoader() {
    assertNotNull(Foo.class.getClassLoader().getResource("foo/main.txt"));
  }

  @Test
  void getTestResourceViaClassLoader() {
    assertNotNull(Foo.class.getClassLoader().getResource("foo/test.txt"));
  }
}
