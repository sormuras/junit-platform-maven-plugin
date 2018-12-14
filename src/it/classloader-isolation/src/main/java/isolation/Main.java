package isolation;

class Main {

  void lookupTestClass() {
    String name = "/isolation/MainTest.class";
    assert getClass().getResourceAsStream(name) == null;
  }

  void lookupEngineClass() {
    String name = "/org/junit/jupiter/engine/JupiterTestEngine.class";
    assert getClass().getResourceAsStream(name) == null;
  }
}
