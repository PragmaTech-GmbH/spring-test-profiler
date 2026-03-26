package digital.pragmatech.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleUnitTest {

  @Test
  void testSimpleAssertion() {
    assertEquals("Hello", "Hello");
  }

  @Test
  void testArithmetic() {
    assertEquals(4, 2 + 2);
  }

  @Test
  void testBoolean() {
    assertTrue(true);
  }
}
