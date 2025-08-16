package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for BookService using Mockito
 * This is a pure unit test without Spring context
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock
  private BookRepository bookRepository;

  @InjectMocks
  private BookService bookService;

  @Test
  void testCreateBook() {
    Book inputBook = new Book("Test Book", "Test Author", "978-1234567890",
      new BigDecimal("25.99"), BookCategory.FICTION);
    Book savedBook = new Book("Test Book", "Test Author", "978-1234567890",
      new BigDecimal("25.99"), BookCategory.FICTION);
    savedBook.setId(1L);

    when(bookRepository.existsByIsbn("978-1234567890")).thenReturn(false);
    when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

    Book result = bookService.createBook(inputBook);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("Test Book", result.getTitle());
    verify(bookRepository).existsByIsbn("978-1234567890");
    verify(bookRepository).save(inputBook);
  }

  @Test
  void testCreateBookWithDuplicateIsbn() {
    Book book = new Book("Test Book", "Test Author", "978-1234567890",
      new BigDecimal("25.99"), BookCategory.FICTION);

    when(bookRepository.existsByIsbn("978-1234567890")).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> bookService.createBook(book));
    verify(bookRepository).existsByIsbn("978-1234567890");
    verify(bookRepository, never()).save(any());
  }

  @Test
  void testFindById() {
    Book book = new Book("Found Book", "Found Author", "978-1111111111",
      new BigDecimal("30.99"), BookCategory.TECHNOLOGY);
    book.setId(1L);

    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

    Optional<Book> result = bookService.findById(1L);

    assertTrue(result.isPresent());
    assertEquals("Found Book", result.get().getTitle());
    verify(bookRepository).findById(1L);
  }

  @Test
  void testFindByIdNotFound() {
    when(bookRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Book> result = bookService.findById(999L);

    assertFalse(result.isPresent());
    verify(bookRepository).findById(999L);
  }

  @Test
  void testFindByAuthor() {
    Book book1 = new Book("Book 1", "John Smith", "978-1111111111",
      new BigDecimal("25.99"), BookCategory.FICTION);
    Book book2 = new Book("Book 2", "John Smith", "978-2222222222",
      new BigDecimal("29.99"), BookCategory.FICTION);

    when(bookRepository.findByAuthorContainingIgnoreCase("John Smith"))
      .thenReturn(Arrays.asList(book1, book2));

    List<Book> result = bookService.findByAuthor("John Smith");

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(book -> book.getAuthor().equals("John Smith")));
    verify(bookRepository).findByAuthorContainingIgnoreCase("John Smith");
  }

  @Test
  void testCountBooks() {
    when(bookRepository.count()).thenReturn(42L);

    long result = bookService.countBooks();

    assertEquals(42L, result);
    verify(bookRepository).count();
  }

  @Test
  void testCountByCategory() {
    when(bookRepository.countByCategory(BookCategory.TECHNOLOGY)).thenReturn(15L);

    long result = bookService.countByCategory(BookCategory.TECHNOLOGY);

    assertEquals(15L, result);
    verify(bookRepository).countByCategory(BookCategory.TECHNOLOGY);
  }

  @Test
  void testDeleteBook() {
    when(bookRepository.existsById(1L)).thenReturn(true);

    assertDoesNotThrow(() -> bookService.deleteBook(1L));

    verify(bookRepository).existsById(1L);
    verify(bookRepository).deleteById(1L);
  }

  @Test
  void testDeleteNonExistentBook() {
    when(bookRepository.existsById(999L)).thenReturn(false);

    assertThrows(IllegalArgumentException.class, () -> bookService.deleteBook(999L));

    verify(bookRepository).existsById(999L);
    verify(bookRepository, never()).deleteById(any());
  }
}
