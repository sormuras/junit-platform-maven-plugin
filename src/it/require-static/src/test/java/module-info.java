// Same(!) name as "main" module descriptor. Most IDEs will choke, though.
// You may "open" module here to allow reflective access of testing frameworks.
module logging {
  // copied from "main" module descriptor
  exports logging;

  // copied from "main" module descriptor
  requires org.slf4j; // not(!) static here, let the module system resolve it

  // test-only modules
  requires junit; // JUnit 4 API for testing
  requires org.apache.logging.log4j; // needed by transitive dependency
}
