package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parallel Integration test for BookService - Set 2
 * Tests service layer operations - SHARES CONTEXT with BookRepositoryParallelTest (same configuration)
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@Transactional
@Execution(ExecutionMode.CONCURRENT)
public class BookServiceParallelIT {

  @Autowired
  private BookService bookService;

  @Test
  void testCreateBookParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(95);

    Book book = new Book("Domain-Driven Design", "Eric Evans", "978-0321125215",
      new BigDecimal("56.99"), BookCategory.TECHNOLOGY);

    Book createdBook = bookService.createBook(book);

    assertNotNull(createdBook.getId());
    assertEquals("Domain-Driven Design", createdBook.getTitle());
  }

  @Test
  void testUpdateBookParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(85);

    Book book = new Book("Original Title", "Author", "978-4444444444",
      new BigDecimal("30.00"), BookCategory.TECHNOLOGY);
    Book savedBook = bookService.createBook(book);

    savedBook.setTitle("Updated Title");
    savedBook.setPrice(new BigDecimal("35.00"));

    Book updatedBook = bookService.updateBook(savedBook.getId(), savedBook);

    assertEquals("Updated Title", updatedBook.getTitle());
    assertEquals(new BigDecimal("35.00"), updatedBook.getPrice());
  }

  @Test
  void testFindByTitleParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(105);

    Book book = new Book("Refactoring: Improving Design", "Martin Fowler", "978-0134757599",
      new BigDecimal("47.99"), BookCategory.TECHNOLOGY);
    bookService.createBook(book);

    List<Book> books = bookService.findByTitle("Refactoring");

    assertEquals(1, books.size());
    assertEquals("Refactoring: Improving Design", books.get(0).getTitle());
  }

  @Test
  void testDeleteBookParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(75);

    Book book = new Book("Temporary Book", "Author", "978-5555555555",
      new BigDecimal("25.00"), BookCategory.TECHNOLOGY);
    Book savedBook = bookService.createBook(book);

    assertNotNull(bookService.findById(savedBook.getId()).orElse(null));

    bookService.deleteBook(savedBook.getId());

    assertTrue(bookService.findById(savedBook.getId()).isEmpty());
  }
}
