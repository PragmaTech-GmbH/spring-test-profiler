package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BAD EXAMPLE: This test uses different TestPropertySource values,
 * causing a context cache MISS because the configuration is different
 * from other tests, even though the difference is minimal.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
  properties = "server.port=8081") // BAD: Different port
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles({"test", "integration"}) // BAD: Different profiles
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:badtest2;DB_CLOSE_DELAY=-1", // Different DB name
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.show-sql=true", // Different JPA setting
  "management.endpoints.web.exposure.include=health,info" // Additional property
})
public class BadTwoIT {

  @Autowired
  private BookService bookService;

  @Test
  void testServiceCreateBook() {
    Book book = new Book("Design Patterns", "Gang of Four", "978-0201633612",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    Book created = bookService.createBook(book);
    assertNotNull(created.getId());
    assertEquals("Design Patterns", created.getTitle());
  }

  @Test
  void testServiceFindByAuthor() {
    Book book1 = new Book("Refactoring", "Martin Fowler", "978-0134757599",
      new BigDecimal("47.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("UML Distilled", "Martin Fowler", "978-0321193681",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    bookService.createBook(book1);
    bookService.createBook(book2);

    var fowlerBooks = bookService.findByAuthor("Martin Fowler");
    assertEquals(2, fowlerBooks.size());
  }

  @Test
  void testServiceCountByCategory() {
    Book book = new Book("Dune", "Frank Herbert", "978-0441172719",
      new BigDecimal("16.99"), BookCategory.FICTION);
    bookService.createBook(book);

    long fictionCount = bookService.countByCategory(BookCategory.FICTION);
    assertEquals(1, fictionCount);
  }
}
