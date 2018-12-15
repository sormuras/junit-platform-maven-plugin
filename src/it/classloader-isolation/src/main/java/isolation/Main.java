package isolation;

class Main {

  void lookupTestClass() {
    String name = "/isolation/MainTests.class";
    if (getClass().getResourceAsStream(name) != null) {
      throw new AssertionError(getClass() + " should not see " + name);
    }
  }

  void lookupEngineClass() {
    String name = "/org/junit/jupiter/engine/JupiterTestEngine.class";
    if (getClass().getResourceAsStream(name) != null) {
      throw new AssertionError(getClass() + " should not see " + name);
    }
  }
}
