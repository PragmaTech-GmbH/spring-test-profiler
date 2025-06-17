package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
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
 * Parallel Integration test for BookRepository - Set 1
 * Tests basic repository operations with JUnit parallel execution
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@Transactional
@Execution(ExecutionMode.CONCURRENT)
public class BookRepositoryParallelIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testSaveAndFindBookParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(120);

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
    Thread.sleep(100);

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
    Thread.sleep(90);

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
    Thread.sleep(110);

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
