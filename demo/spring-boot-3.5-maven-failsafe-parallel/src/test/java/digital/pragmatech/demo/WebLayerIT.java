package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Integration test for Web Layer - Part 3
 * Tests REST endpoints with different configuration - DIFFERENT CONTEXT (webEnvironment, different port)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:weblayertest;DB_CLOSE_DELAY=-1",
  "server.port=0"  // Different configuration creates new context
})
public class WebLayerIT {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testCreateBookEndpoint() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(100);

    String uniqueIsbn = "978-0596529" + System.currentTimeMillis() % 1000;
    Book book = new Book("RESTful Web Services", "Leonard Richardson", uniqueIsbn,
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    ResponseEntity<Book> response = testRestTemplate.postForEntity("/api/books", book, Book.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("RESTful Web Services", response.getBody().getTitle());
  }

  @Test
  void testGetAllBooksEndpoint() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(120);

    // First create a book
    String uniqueIsbn = "978-1617295" + System.currentTimeMillis() % 1000;
    Book book = new Book("API Design Patterns", "JJ Geewax", uniqueIsbn,
      new BigDecimal("49.99"), BookCategory.TECHNOLOGY);
    testRestTemplate.postForEntity("/api/books", book, Book.class);

    ResponseEntity<Book[]> response = testRestTemplate.getForEntity("/api/books", Book[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);
  }

  @Test
  void testGetBookCountEndpoint() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(80);

    ResponseEntity<Long> response = testRestTemplate.getForEntity("/api/books/count", Long.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() >= 0);
  }

  @Test
  void testSearchBooksEndpoint() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(140);

    // Create a book first
    String uniqueAuthor = "Kent Beck " + System.currentTimeMillis();
    String uniqueIsbn = "978-0321146" + System.currentTimeMillis() % 1000;
    Book book = new Book("Test Driven Development", uniqueAuthor, uniqueIsbn,
      new BigDecimal("42.99"), BookCategory.TECHNOLOGY);
    testRestTemplate.postForEntity("/api/books", book, Book.class);

    ResponseEntity<Book[]> response = testRestTemplate.getForEntity(
      "/api/books/search?author=" + uniqueAuthor, Book[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }
}
