package bar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ReflectionBlackBoxTests {

  private final ClassLoader loader = getClass().getClassLoader();

  @Test
  void reflectFooClass() throws Exception {
    assertDoesNotThrow(() -> loader.loadClass("foo.Foo"));
  }

  @Test
  void reflectInternalPublicClass() throws Exception {
    // does not throw: https://github.com/sormuras/junit-platform-maven-plugin/issues/6
    Class<?> internal = loader.loadClass("foo.internal.InternalPublic");
    assertThrows(IllegalAccessException.class, () -> internal.getConstructor().newInstance());
  }

  @Test
  void reflectClassThatDoesNotExist() throws Exception {
    assertThrows(ClassNotFoundException.class, () -> loader.loadClass("ClassThatDoesNotExist"));
  }
}
