package bar;

import static org.junit.jupiter.api.Assertions.*;

import foo.*;
import org.junit.jupiter.api.*;

class BarTests {

  @Test
  void accessModuleBar() {
    assertEquals("bar", getClass().getModule().getName(), "Class does not reside in module 'bar'!");
  }

  @Test
  void accessModuleFoo() {
    assertEquals("foo", Foo.class.getModule().getName(), "Class does not reside in module 'bar'!");
  }
}
