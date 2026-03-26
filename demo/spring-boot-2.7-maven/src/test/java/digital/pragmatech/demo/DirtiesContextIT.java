package digital.pragmatech.demo;

import java.math.BigDecimal;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BAD EXAMPLE: Uses @DirtiesContext and custom TestPropertySource, forcing
 * a fresh context for every test method. This is a cache MISS every time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:dirtydb;DB_CLOSE_DELAY=-1",
    "spring.jpa.show-sql=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DirtiesContextIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testSaveBook() {
    Book book = new Book("Spring in Action", "Craig Walls", "978-1617294945",
        new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);
    assertNotNull(saved.getId());
  }

  @Test
  void testExistsByIsbn() {
    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
        new BigDecimal("52.99"), BookCategory.TECHNOLOGY);
    bookRepository.save(book);

    assertTrue(bookRepository.existsByIsbn("978-0134685991"));
  }
}
