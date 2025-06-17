package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
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
 * Integration test for BookRepository - Part 1
 * Tests basic repository operations
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@Transactional
public class BookRepositoryIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testSaveAndFindBook() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(100);

    Book book = new Book("Clean Code", "Robert C. Martin", "978-0132350884",
      new BigDecimal("45.99"), BookCategory.TECHNOLOGY);

    Book savedBook = bookRepository.save(book);

    assertNotNull(savedBook.getId());
    assertEquals("Clean Code", savedBook.getTitle());
    assertEquals("Robert C. Martin", savedBook.getAuthor());
  }

  @Test
  void testFindByIsbn() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(150);

    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
      new BigDecimal("52.99"), BookCategory.TECHNOLOGY);
    bookRepository.save(book);

    Book foundBook = bookRepository.findByIsbn("978-0134685991").orElse(null);

    assertNotNull(foundBook);
    assertEquals("Effective Java", foundBook.getTitle());
  }

  @Test
  void testFindByCategory() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(120);

    Book techBook = new Book("Design Patterns", "Gang of Four", "978-0201633612",
      new BigDecimal("59.99"), BookCategory.TECHNOLOGY);
    Book fictionBook = new Book("1984", "George Orwell", "978-0451524935",
      new BigDecimal("12.99"), BookCategory.FICTION);

    bookRepository.save(techBook);
    bookRepository.save(fictionBook);

    List<Book> techBooks = bookRepository.findByCategory(BookCategory.TECHNOLOGY);

    assertEquals(1, techBooks.size());
    assertEquals("Design Patterns", techBooks.get(0).getTitle());
  }

  @Test
  void testCountByCategory() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(80);

    Book book1 = new Book("Java Concurrency", "Brian Goetz", "978-0321349606",
      new BigDecimal("49.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Spring in Action", "Craig Walls", "978-1617294945",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);

    bookRepository.save(book1);
    bookRepository.save(book2);

    long count = bookRepository.countByCategory(BookCategory.TECHNOLOGY);

    assertEquals(2, count);
  }
}
