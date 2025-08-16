package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parallel Integration test for BookService - Set 2
 * Tests service layer operations - SHARES CONTEXT with BookRepositoryParallelTest (same configuration)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Execution(ExecutionMode.CONCURRENT)
class BookServiceParallelIT {

  @Autowired
  private BookService bookService;

  @Test
  void testCreateBookParallel() {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(95, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book = new Book("Domain-Driven Design", "Eric Evans", "978-0321125215",
      new BigDecimal("56.99"), BookCategory.TECHNOLOGY);

    Book createdBook = bookService.createBook(book);

    assertNotNull(createdBook.getId());
    assertEquals("Domain-Driven Design", createdBook.getTitle());
  }

  @Test
  void testUpdateBookParallel() {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(85, TimeUnit.MILLISECONDS)
      .until(() -> true);

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
  void testFindByTitleParallel() {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(105, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book = new Book("Refactoring: Improving Design", "Martin Fowler", "978-0134757599",
      new BigDecimal("47.99"), BookCategory.TECHNOLOGY);
    bookService.createBook(book);

    List<Book> books = bookService.findByTitle("Refactoring");

    assertEquals(1, books.size());
    assertEquals("Refactoring: Improving Design", books.get(0).getTitle());
  }

  @Test
  void testDeleteBookParallel() {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(75, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book = new Book("Temporary Book", "Author", "978-5555555555",
      new BigDecimal("25.00"), BookCategory.TECHNOLOGY);
    Book savedBook = bookService.createBook(book);

    assertNotNull(bookService.findById(savedBook.getId()).orElse(null));

    bookService.deleteBook(savedBook.getId());

    assertTrue(bookService.findById(savedBook.getId()).isEmpty());
  }
}
