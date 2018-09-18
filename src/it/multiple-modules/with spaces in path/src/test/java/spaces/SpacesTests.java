package spaces;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class SpacesTests {

  @Test
  void moduleSpaces() {
    assertEquals("spaces", Spaces.class.getModule().getName());
  }
}
