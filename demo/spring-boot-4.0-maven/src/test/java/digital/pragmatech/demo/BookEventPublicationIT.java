package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.time.Duration;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.notification.BookCreatedNotifier;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Spring Modulith's JDBC-backed event publication registry processes a domain event
 * end-to-end: BookService publishes a BookCreatedEvent on commit and the @ApplicationModuleListener
 * in the notification module handles it asynchronously.
 *
 * <p>Shares the context configuration of {@link GoodIT} (cache HIT). Deliberately
 * NOT @Transactional: @ApplicationModuleListener only fires after the transaction commits, and
 * BookService.createBook commits its own transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookEventPublicationIT {

  @Autowired
  private BookService bookService;

  @Autowired
  private BookCreatedNotifier bookCreatedNotifier;

  @AfterEach
  void cleanUp() {
    // This test commits for real (no rollback), so remove the book to keep the shared
    // context's database consistent for other test classes (e.g. GoodIT's count assertions)
    bookService.deleteByIsbn("978-1098157630");
  }

  @Test
  void bookCreationPublishesEventToModuleListener() {
    bookService.createBook(new Book("Modular Monoliths", "Kamil Grzybek", "978-1098157630",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY));

    await().atMost(Duration.ofSeconds(10))
      .untilAsserted(() ->
        assertTrue(bookCreatedNotifier.notifiedIsbns().contains("978-1098157630")));
  }
}
