package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Data Layer - Part 4
 * Tests only JPA layer with @DataJpaTest - DIFFERENT CONTEXT (test slice)
 */
@DataJpaTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:datatest;DB_CLOSE_DELAY=-1",
  "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class DataLayerIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testEntityPersistence() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(90);

    Book book = new Book("Hibernate in Action", "Christian Bauer", "978-1932394153",
      new BigDecimal("48.99"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);

    assertNotNull(saved.getId());
    assertEquals("Hibernate in Action", saved.getTitle());
    assertEquals("Christian Bauer", saved.getAuthor());
  }

  @Test
  void testFindByTitleContaining() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(110);

    Book book1 = new Book("Spring Framework Guide", "Author", "978-5555555555",
      new BigDecimal("35.00"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("Java Spring Boot", "Author", "978-6666666666",
      new BigDecimal("40.00"), BookCategory.TECHNOLOGY);
    Book book3 = new Book("Python Basics", "Author", "978-7777777777",
      new BigDecimal("30.00"), BookCategory.TECHNOLOGY);

    bookRepository.save(book1);
    bookRepository.save(book2);
    bookRepository.save(book3);

    List<Book> springBooks = bookRepository.findByTitleContainingIgnoreCase("Spring");

    assertEquals(2, springBooks.size());
    assertTrue(springBooks.stream().anyMatch(b -> b.getTitle().contains("Spring Framework")));
    assertTrue(springBooks.stream().anyMatch(b -> b.getTitle().contains("Java Spring")));
  }

  @Test
  void testExistsById() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(70);

    Book book = new Book("JPA Essentials", "Author", "978-8888888888",
      new BigDecimal("42.00"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);

    assertTrue(bookRepository.existsById(saved.getId()));
    assertFalse(bookRepository.existsById(999L));
  }

  @Test
  void testDeleteByIsbn() throws InterruptedException {
    // Simulate some processing time
    Thread.sleep(130);

    Book book = new Book("Database Design", "Author", "978-9999999999",
      new BigDecimal("38.00"), BookCategory.TECHNOLOGY);

    bookRepository.save(book);

    assertTrue(bookRepository.existsByIsbn("978-9999999999"));

    bookRepository.deleteByIsbn("978-9999999999");

    Optional<Book> deleted = bookRepository.findByIsbn("978-9999999999");
    assertTrue(deleted.isEmpty());
  }
}
