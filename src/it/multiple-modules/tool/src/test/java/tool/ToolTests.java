package tool;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ToolTests {

  @Test
  void moduleTool() {
    assertEquals("tool", Tool.class.getModule().getName());
  }
}
