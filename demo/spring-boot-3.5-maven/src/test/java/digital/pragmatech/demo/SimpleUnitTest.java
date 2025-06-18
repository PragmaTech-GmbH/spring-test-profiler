package digital.pragmatech.demo;

import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Spring unit test to demonstrate surefire phase report generation.
 * Unit tests (not ending with IT) are run by maven-surefire-plugin.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:unittest")
public class SimpleUnitTest {

    @MockBean
    private BookService bookService;

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