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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parallel Integration test for Database Layer - Set 4
 * Tests only JPA layer with @DataJpaTest - DIFFERENT CONTEXT (test slice with TestEntityManager)
 */
@DataJpaTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:paralleldbtest;DB_CLOSE_DELAY=-1",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.show-sql=true"  // Different configuration
})
@Execution(ExecutionMode.CONCURRENT)
public class DatabaseParallelIT {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testEntityManagerPersistenceParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(78);

    Book book = new Book("Effective Java 3rd Edition", "Joshua Bloch", "978-0134685991",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    Book persistedBook = entityManager.persistAndFlush(book);

    assertNotNull(persistedBook.getId());
    assertEquals("Effective Java 3rd Edition", persistedBook.getTitle());
  }

  @Test
  void testRepositoryCountParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(92);

    Book book1 = new Book("Book A", "Author A", "978-1111111111",
      new BigDecimal("25.00"), BookCategory.FICTION);
    Book book2 = new Book("Book B", "Author B", "978-2222222222",
      new BigDecimal("30.00"), BookCategory.FICTION);

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);

    long count = bookRepository.count();

    assertEquals(2, count);
  }

  @Test
  void testQueryByExampleParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(108);

    Book techBook1 = new Book("Spring Boot Guide", "Author", "978-3333333333",
      new BigDecimal("45.00"), BookCategory.TECHNOLOGY);
    Book techBook2 = new Book("Spring Security", "Author", "978-4444444444",
      new BigDecimal("50.00"), BookCategory.TECHNOLOGY);
    Book fictionBook = new Book("Novel", "Author", "978-5555555555",
      new BigDecimal("20.00"), BookCategory.FICTION);

    entityManager.persistAndFlush(techBook1);
    entityManager.persistAndFlush(techBook2);
    entityManager.persistAndFlush(fictionBook);

    List<Book> techBooks = bookRepository.findByCategory(BookCategory.TECHNOLOGY);

    assertEquals(2, techBooks.size());
    assertTrue(techBooks.stream().allMatch(b -> b.getCategory() == BookCategory.TECHNOLOGY));
  }

  @Test
  void testTransactionalBehaviorParallel() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(135);

    Book book = new Book("Transaction Test", "Author", "978-6666666666",
      new BigDecimal("35.00"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);

    // Clear the persistence context
    entityManager.clear();

    // Verify the book is still persisted
    Book found = bookRepository.findById(saved.getId()).orElse(null);

    assertNotNull(found);
    assertEquals("Transaction Test", found.getTitle());
  }
}
