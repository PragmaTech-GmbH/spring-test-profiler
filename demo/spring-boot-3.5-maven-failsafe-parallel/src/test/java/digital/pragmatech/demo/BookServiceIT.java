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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for BookService - Part 2
 * Tests service layer operations - SHARES CONTEXT with BookRepositoryIT (same configuration)
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@Transactional
public class BookServiceIT {

  @Autowired
  private BookService bookService;

  @Test
  void testCreateBook() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(90);

    Book book = new Book("Microservices Patterns", "Chris Richardson", "978-1617294549",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    Book createdBook = bookService.createBook(book);

    assertNotNull(createdBook.getId());
    assertEquals("Microservices Patterns", createdBook.getTitle());
  }

  @Test
  void testFindByAuthor() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(110);

    String uniqueAuthor = "Craig Walls " + System.currentTimeMillis();
    Book book = new Book("Spring Boot in Action", uniqueAuthor, "978-1617292545",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);
    bookService.createBook(book);

    List<Book> books = bookService.findByAuthor(uniqueAuthor);

    assertEquals(1, books.size());
    assertEquals("Spring Boot in Action", books.get(0).getTitle());
  }

  @Test
  void testFindByPriceRange() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(130);

    Book book1 = new Book("Affordable Book", "Author One", "978-1111111111",
      new BigDecimal("25.00"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Expensive Book", "Author Two", "978-2222222222",
      new BigDecimal("75.00"), BookCategory.TECHNOLOGY);

    bookService.createBook(book1);
    bookService.createBook(book2);

    List<Book> books = bookService.findByPriceRange(new BigDecimal("20.00"), new BigDecimal("30.00"));

    assertEquals(1, books.size());
    assertEquals("Affordable Book", books.get(0).getTitle());
  }

  @Test
  void testCountBooks() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(70);

    Book book1 = new Book("Book One", "Author", "978-3333333333",
      new BigDecimal("30.00"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Book Two", "Author", "978-4444444444",
      new BigDecimal("35.00"), BookCategory.FICTION);

    bookService.createBook(book1);
    bookService.createBook(book2);

    long count = bookService.countBooks();

    assertTrue(count >= 2);
  }
}
