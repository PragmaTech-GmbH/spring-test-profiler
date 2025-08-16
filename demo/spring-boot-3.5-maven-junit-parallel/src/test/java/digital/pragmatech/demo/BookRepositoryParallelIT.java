package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Parallel Integration test for BookRepository - Set 1
 * Tests basic repository operations with JUnit parallel execution
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Execution(ExecutionMode.CONCURRENT)
class BookRepositoryParallelIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testSaveAndFindBookParallel() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(120, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book = new Book("Clean Architecture", "Robert C. Martin", "978-0134494166",
      new BigDecimal("48.99"), BookCategory.TECHNOLOGY);

    Book savedBook = bookRepository.save(book);

    assertNotNull(savedBook.getId());
    assertEquals("Clean Architecture", savedBook.getTitle());
    assertEquals("Robert C. Martin", savedBook.getAuthor());
  }

  @Test
  void testFindByIsbnParallel() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(100, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book = new Book("Java: The Complete Reference", "Herbert Schildt", "978-1260440232",
      new BigDecimal("59.99"), BookCategory.TECHNOLOGY);
    bookRepository.save(book);

    Book foundBook = bookRepository.findByIsbn("978-1260440232").orElse(null);

    assertNotNull(foundBook);
    assertEquals("Java: The Complete Reference", foundBook.getTitle());
  }

  @Test
  void testFindByAuthorParallel() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(90, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book1 = new Book("Spring Security in Action", "Laurentiu Spilca", "978-1617297731",
      new BigDecimal("49.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Spring Microservices", "Laurentiu Spilca", "978-1234567890",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    bookRepository.save(book1);
    bookRepository.save(book2);

    List<Book> books = bookRepository.findByAuthorContainingIgnoreCase("Laurentiu");

    assertEquals(2, books.size());
  }

  @Test
  void testFindByPriceBetweenParallel() throws InterruptedException {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(110, TimeUnit.MILLISECONDS)
      .until(() -> true);

    Book book1 = new Book("Budget Book", "Author", "978-1111111111",
      new BigDecimal("15.00"), BookCategory.FICTION);
    Book book2 = new Book("Mid-range Book", "Author", "978-2222222222",
      new BigDecimal("35.00"), BookCategory.FICTION);
    Book book3 = new Book("Premium Book", "Author", "978-3333333333",
      new BigDecimal("80.00"), BookCategory.FICTION);

    bookRepository.save(book1);
    bookRepository.save(book2);
    bookRepository.save(book3);

    List<Book> books = bookRepository.findByPriceBetween(new BigDecimal("20.00"), new BigDecimal("50.00"));

    assertEquals(1, books.size());
    assertEquals("Mid-range Book", books.get(0).getTitle());
  }
}
