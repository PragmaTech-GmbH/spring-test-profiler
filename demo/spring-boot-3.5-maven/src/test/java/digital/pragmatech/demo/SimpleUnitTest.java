package digital.pragmatech.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple Spring unit test to demonstrate surefire phase report generation.
 * Unit tests (not ending with IT) are run by maven-surefire-plugin.
 */
class SimpleUnitTest {

  @Test
  void testSimpleAssertion() {
    String message = "Hello, World!";
    assertEquals("Hello, World!", message);
  }

  @Test
  void testArithmetic() {
    int result = 2 + 2;
    assertEquals(4, result);
  }

  @Test
  void testBooleanLogic() {
    assertTrue(true);
    assertFalse(false);
  }
}
