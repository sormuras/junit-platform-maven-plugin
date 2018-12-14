package isolation;

public class Main {

  public void lookupTestClass() {
    String name = "/isolation/MainTest.class";
    if (getClass().getResourceAsStream(name) != null) {
      throw new AssertionError(getClass() + " should not see " + name);
    }
  }

  public void lookupEngineClass() {
    String name = "/org/junit/jupiter/engine/JupiterTestEngine.class";
    if (getClass().getResourceAsStream(name) != null) {
      throw new AssertionError(getClass() + " should not see " + name);
    }
  }
}
