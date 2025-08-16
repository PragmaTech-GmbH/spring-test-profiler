package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Web Layer - Part 3
 * Tests REST endpoints with different configuration - DIFFERENT CONTEXT (webEnvironment, different port)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:weblayertest;DB_CLOSE_DELAY=-1",
  "server.port=0"  // Different configuration creates new context
})
class WebLayerIT {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testCreateBookEndpoint() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(100, TimeUnit.MILLISECONDS)
      .until(() -> true);

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
    Awaitility.await()
      .pollDelay(120, TimeUnit.MILLISECONDS)
      .until(() -> true);

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
    Awaitility.await()
      .pollDelay(80, TimeUnit.MILLISECONDS)
      .until(() -> true);

    ResponseEntity<Long> response = testRestTemplate.getForEntity("/api/books/count", Long.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() >= 0);
  }

  @Test
  void testSearchBooksEndpoint() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(140, TimeUnit.MILLISECONDS)
      .until(() -> true);

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
