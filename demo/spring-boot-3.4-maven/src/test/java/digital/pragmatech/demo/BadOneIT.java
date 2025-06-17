package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BAD EXAMPLE: This test uses @DirtiesContext unnecessarily,
 * causing the Spring context to be recreated for every test method.
 * This is a cache MISS every time!
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:badtest1;DB_CLOSE_DELAY=-1",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "logging.level.org.springframework.web=DEBUG"  // Different logging config
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // BAD: Forces context reload
public class BadOneIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testCreateBook() {
    Book book = new Book("Spring in Action", "Craig Walls", "978-1617294945",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);
    assertNotNull(saved.getId());
    assertEquals("Spring in Action", saved.getTitle());
  }

  @Test
  void testFindByCategory() {
    Book book1 = new Book("Clean Code", "Robert Martin", "978-0132350884",
      new BigDecimal("45.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("The Hobbit", "J.R.R. Tolkien", "978-0547928227",
      new BigDecimal("12.99"), BookCategory.FANTASY);

    bookRepository.save(book1);
    bookRepository.save(book2);

    var techBooks = bookRepository.findByCategory(BookCategory.TECHNOLOGY);
    assertEquals(1, techBooks.size());
    assertEquals("Clean Code", techBooks.get(0).getTitle());
  }

  @Test
  void testCountBooks() {
    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
      new BigDecimal("52.99"), BookCategory.TECHNOLOGY);
    bookRepository.save(book);

    long count = bookRepository.count();
    assertEquals(1, count);
  }
}
