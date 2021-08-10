package sut.module;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dummymodule.module.one.AppOne;
import dummymodule.module.two.AppTwo;
import org.junit.jupiter.api.Test;

class SubjectUnderTest {
  @Test
  void test_dummy_module_one_in_compile_scope_is_accessible() {
    assertTrue(new AppOne().returnTrue());
  }

  @Test
  void test_dummy_module_two_in_provided_scope_is_accessible() {
    assertTrue(new AppTwo().returnTrue());
  }
}
