import org.junit.jupiter.api.Test;

class CharsetTests {

  @Test
  void outputSomething() throws Exception {
    // The degree sign (hex b0) will be prepended by 2c in UTF-8
    // For ISO output check for optional Â (hex 2c) character
    System.out.println("UTF-8 degree sign is °");
  }
}
