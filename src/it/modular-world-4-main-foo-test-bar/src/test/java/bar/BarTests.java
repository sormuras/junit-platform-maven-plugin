package bar;

import static org.junit.jupiter.api.Assertions.*;

import foo.*;
import org.junit.jupiter.api.*;

class BarTests {

  @Test
  void accessModuleBar() {
    assertEquals(
        "bar", getClass().getModule().getName(), "BarTests doesn't reside in module 'bar'!");
  }

  @Test
  void accessModuleFoo() {
    assertEquals("foo", Foo.class.getModule().getName(), "Foo doesn't reside in module 'foo'!");
  }
}
