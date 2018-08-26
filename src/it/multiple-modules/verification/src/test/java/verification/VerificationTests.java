package verification;

import static org.junit.jupiter.api.Assertions.*;

import application.*;
import org.junit.jupiter.api.*;
import tool.*;

class VerificationTests {

  @Test
  void accessApplicationModule() {
    assertEquals("application", Application.class.getModule().getName());
    // Helper not visible here, .... Helper.class.getModule().getName()
  }

  @Test
  void accessToolModule() {
    assertEquals("tool", Tool.class.getModule().getName());
  }
}
