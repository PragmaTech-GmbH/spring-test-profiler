package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BAD EXAMPLE: This test uses @AutoConfigureWebMvc annotation unnecessarily,
 * and has yet another different configuration, causing another context cache MISS.
 * Also uses webEnvironment.RANDOM_PORT but different from other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc // BAD: Unnecessary annotation that changes context
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test") // Same as first test, but other config differs
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:badtest3;DB_CLOSE_DELAY=-1", // Different DB name again
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "server.servlet.context-path=/api/v1", // BAD: Different context path
  "spring.jackson.property-naming-strategy=SNAKE_CASE" // BAD: Different Jackson config
})
public class ThirdBadIT {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testCreateBookViaRestApi() {
    Book book = new Book("Java: The Complete Reference", "Herbert Schildt",
      "978-1260440232", new BigDecimal("59.99"), BookCategory.TECHNOLOGY);

    ResponseEntity<Book> response = testRestTemplate.postForEntity("/api/books", book, Book.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Java: The Complete Reference", response.getBody().getTitle());
  }

  @Test
  void testGetAllBooksViaRestApi() {
    // First create a book
    Book book = new Book("Spring Boot in Action", "Craig Walls", "978-1617292545",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);
    testRestTemplate.postForEntity("/api/books", book, Book.class);

    ResponseEntity<Book[]> response = testRestTemplate.getForEntity("/api/books", Book[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);
  }

  @Test
  void testGetBookCountViaRestApi() {
    ResponseEntity<Long> response = testRestTemplate.getForEntity("/api/books/count", Long.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() >= 0);
  }
}
