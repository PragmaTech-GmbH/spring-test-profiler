package digital.pragmatech.demo;

import java.math.BigDecimal;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GOOD EXAMPLE: This test class uses consistent configuration that can be
 * shared with other tests, resulting in context cache HITs.
 * Uses @Transactional to clean up data instead of @DirtiesContext.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional // GOOD: Rolls back after each test, no context recreation needed
public class GoodIT {

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private BookService bookService;

  @Test
  void testRepositoryAndService() {
    // Test both repository and service in one test class with same context
    Book book = new Book("Clean Architecture", "Robert C. Martin", "978-0134494166",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    // Test repository
    Book savedViaRepo = bookRepository.save(book);
    assertNotNull(savedViaRepo.getId());

    // Test service (reusing same context)
    var foundBooks = bookService.findByAuthor("Robert C. Martin");
    assertEquals(1, foundBooks.size());
    assertEquals("Clean Architecture", foundBooks.get(0).getTitle());
  }

  @Test
  void testAnotherOperationSameContext() {
    // This test will reuse the same Spring context as the previous test
    Book book1 = new Book("Microservices Patterns", "Chris Richardson", "978-1617294549",
      new BigDecimal("49.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Domain-Driven Design", "Eric Evans", "978-0321125217",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    bookService.createBook(book1);
    bookService.createBook(book2);

    long techBookCount = bookService.countByCategory(BookCategory.TECHNOLOGY);
    assertEquals(2, techBookCount);
  }

  @Test
  void testContextReuseAgain() {
    // Yet another test that will reuse the same context
    Book book = new Book("Building Microservices", "Sam Newman", "978-1492034025",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);

    Book created = bookService.createBook(book);
    var found = bookRepository.findByIsbn("978-1492034025");

    assertTrue(found.isPresent());
    assertEquals(created.getId(), found.get().getId());
  }
}
