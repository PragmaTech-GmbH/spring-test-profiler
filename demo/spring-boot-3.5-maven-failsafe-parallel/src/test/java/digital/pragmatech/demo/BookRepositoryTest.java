package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for BookRepository using @DataJpaTest slice
 * This creates a minimal Spring context with only JPA components
 */
@DataJpaTest
class BookRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testFindByCategory() {
    // Given
    Book techBook1 = new Book("Spring Guide", "Author 1", "978-1111111111",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);
    Book techBook2 = new Book("Java Guide", "Author 2", "978-2222222222",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);
    Book fictionBook = new Book("Novel", "Author 3", "978-3333333333",
      new BigDecimal("19.99"), BookCategory.FICTION);

    entityManager.persistAndFlush(techBook1);
    entityManager.persistAndFlush(techBook2);
    entityManager.persistAndFlush(fictionBook);

    // When
    List<Book> techBooks = bookRepository.findByCategory(BookCategory.TECHNOLOGY);

    // Then
    assertEquals(2, techBooks.size());
    assertTrue(techBooks.stream().allMatch(book -> book.getCategory() == BookCategory.TECHNOLOGY));
  }

  @Test
  void testFindByIsbn() {
    // Given
    String isbn = "978-1234567890";
    Book book = new Book("Test Book", "Test Author", isbn,
      new BigDecimal("25.99"), BookCategory.FICTION);
    entityManager.persistAndFlush(book);

    // When
    Optional<Book> found = bookRepository.findByIsbn(isbn);

    // Then
    assertTrue(found.isPresent());
    assertEquals("Test Book", found.get().getTitle());
    assertEquals(isbn, found.get().getIsbn());
  }

  @Test
  void testExistsByIsbn() {
    // Given
    String isbn = "978-9876543210";
    Book book = new Book("Existing Book", "Existing Author", isbn,
      new BigDecimal("35.99"), BookCategory.TECHNOLOGY);
    entityManager.persistAndFlush(book);

    // When/Then
    assertTrue(bookRepository.existsByIsbn(isbn));
    assertFalse(bookRepository.existsByIsbn("978-0000000000"));
  }

  @Test
  void testFindByAuthorContainingIgnoreCase() {
    // Given
    Book book1 = new Book("Book 1", "John Smith", "978-1111111111",
      new BigDecimal("25.99"), BookCategory.FICTION);
    Book book2 = new Book("Book 2", "john SMITH", "978-2222222222",
      new BigDecimal("29.99"), BookCategory.FICTION);
    Book book3 = new Book("Book 3", "Jane Doe", "978-3333333333",
      new BigDecimal("22.99"), BookCategory.FICTION);

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);
    entityManager.persistAndFlush(book3);

    // When
    List<Book> smithBooks = bookRepository.findByAuthorContainingIgnoreCase("smith");

    // Then
    assertEquals(2, smithBooks.size());
    assertTrue(smithBooks.stream().allMatch(book ->
      book.getAuthor().toLowerCase().contains("smith")));
  }

  @Test
  void testFindByTitleContainingIgnoreCase() {
    // Given
    Book book1 = new Book("Spring in Action", "Craig Walls", "978-1111111111",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);
    Book book2 = new Book("SPRING Boot Guide", "Another Author", "978-2222222222",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);
    Book book3 = new Book("Java Basics", "Java Author", "978-3333333333",
      new BigDecimal("29.99"), BookCategory.TECHNOLOGY);

    entityManager.persistAndFlush(book1);
    entityManager.persistAndFlush(book2);
    entityManager.persistAndFlush(book3);

    // When
    List<Book> springBooks = bookRepository.findByTitleContainingIgnoreCase("spring");

    // Then
    assertEquals(2, springBooks.size());
    assertTrue(springBooks.stream().allMatch(book ->
      book.getTitle().toLowerCase().contains("spring")));
  }

  @Test
  void testCountByCategory() {
    // Given
    Book techBook1 = new Book("Tech Book 1", "Author 1", "978-1111111111",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);
    Book techBook2 = new Book("Tech Book 2", "Author 2", "978-2222222222",
      new BigDecimal("44.99"), BookCategory.TECHNOLOGY);
    Book fictionBook = new Book("Fiction Book", "Author 3", "978-3333333333",
      new BigDecimal("19.99"), BookCategory.FICTION);

    entityManager.persistAndFlush(techBook1);
    entityManager.persistAndFlush(techBook2);
    entityManager.persistAndFlush(fictionBook);

    // When/Then
    assertEquals(2L, bookRepository.countByCategory(BookCategory.TECHNOLOGY));
    assertEquals(1L, bookRepository.countByCategory(BookCategory.FICTION));
    assertEquals(0L, bookRepository.countByCategory(BookCategory.SCIENCE));
  }

  @Test
  void testFindByPriceBetween() {
    // Given
    Book cheapBook = new Book("Cheap Book", "Author 1", "978-1111111111",
      new BigDecimal("15.99"), BookCategory.FICTION);
    Book midBook = new Book("Mid Book", "Author 2", "978-2222222222",
      new BigDecimal("35.99"), BookCategory.TECHNOLOGY);
    Book expensiveBook = new Book("Expensive Book", "Author 3", "978-3333333333",
      new BigDecimal("75.99"), BookCategory.TECHNOLOGY);

    entityManager.persistAndFlush(cheapBook);
    entityManager.persistAndFlush(midBook);
    entityManager.persistAndFlush(expensiveBook);

    // When
    List<Book> midRangeBooks = bookRepository.findByPriceBetween(
      new BigDecimal("20.00"), new BigDecimal("50.00"));

    // Then
    assertEquals(1, midRangeBooks.size());
    assertEquals("Mid Book", midRangeBooks.get(0).getTitle());
  }

  @Test
  void testDeleteByIsbn() {
    // Given
    String isbn = "978-1234567890";
    Book book = new Book("To Delete", "Author", isbn,
      new BigDecimal("25.99"), BookCategory.FICTION);
    entityManager.persistAndFlush(book);
    entityManager.clear(); // Clear the persistence context

    // Verify book exists
    assertTrue(bookRepository.existsByIsbn(isbn));

    // When
    bookRepository.deleteByIsbn(isbn);

    // Then
    assertFalse(bookRepository.existsByIsbn(isbn));
  }
}
