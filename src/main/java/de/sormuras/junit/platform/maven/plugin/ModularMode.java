package de.sormuras.junit.platform.maven.plugin;

public enum ModularMode {

  /** No modules at all -- legacy class-path usage. */
  MAIN_PLAIN_TEST_PLAIN,

  /** Test contains a module descriptor -- */
  MAIN_PLAIN_TEST_MODULE,

  /** Main contains a module descriptors -- needs runtime patching. */
  MAIN_MODULE_TEST_PLAIN,

  /** Main and test contain module descriptors. */
  MAIN_MODULE_TEST_MODULE
}
