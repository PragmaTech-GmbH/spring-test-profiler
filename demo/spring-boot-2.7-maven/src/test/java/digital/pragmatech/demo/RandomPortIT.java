package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Uses RANDOM_PORT web environment with TestRestTemplate, creating yet
 * another distinct context configuration for the profiler to track.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RandomPortIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void testGetAllBooksEndpoint() {
    ResponseEntity<Book[]> response = restTemplate.getForEntity("/api/books", Book[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void testCreateBookEndpoint() {
    Book book = new Book();
    book.setTitle("Test Book");
    book.setAuthor("Test Author");
    book.setIsbn("978-0000000001");

    ResponseEntity<Book> response = restTemplate.postForEntity("/api/books", book, Book.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }
}
