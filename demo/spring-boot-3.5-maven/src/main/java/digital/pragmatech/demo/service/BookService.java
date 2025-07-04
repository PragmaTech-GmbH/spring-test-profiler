package digital.pragmatech.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

  private final BookRepository bookRepository;

  public BookService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  public Book createBook(Book book) {
    if (bookRepository.existsByIsbn(book.getIsbn())) {
      throw new IllegalArgumentException("Book with ISBN " + book.getIsbn() + " already exists");
    }
    return bookRepository.save(book);
  }

  @Transactional(readOnly = true)
  public Optional<Book> findById(Long id) {
    return bookRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public Optional<Book> findByIsbn(String isbn) {
    return bookRepository.findByIsbn(isbn);
  }

  @Transactional(readOnly = true)
  public List<Book> findAll() {
    return bookRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<Book> findByAuthor(String author) {
    return bookRepository.findByAuthorContainingIgnoreCase(author);
  }

  @Transactional(readOnly = true)
  public List<Book> findByTitle(String title) {
    return bookRepository.findByTitleContainingIgnoreCase(title);
  }

  @Transactional(readOnly = true)
  public List<Book> findByCategory(BookCategory category) {
    return bookRepository.findByCategory(category);
  }

  @Transactional(readOnly = true)
  public List<Book> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    return bookRepository.findByPriceBetween(minPrice, maxPrice);
  }

  public Book updateBook(Long id, Book updatedBook) {
    Book existingBook = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

    // Check if ISBN is being changed and if new ISBN already exists
    if (!existingBook.getIsbn().equals(updatedBook.getIsbn()) &&
      bookRepository.existsByIsbn(updatedBook.getIsbn())) {
      throw new IllegalArgumentException("Book with ISBN " + updatedBook.getIsbn() + " already exists");
    }

    existingBook.setTitle(updatedBook.getTitle());
    existingBook.setAuthor(updatedBook.getAuthor());
    existingBook.setIsbn(updatedBook.getIsbn());
    existingBook.setPrice(updatedBook.getPrice());
    existingBook.setPublicationDate(updatedBook.getPublicationDate());
    existingBook.setCategory(updatedBook.getCategory());
    existingBook.setDescription(updatedBook.getDescription());

    return bookRepository.save(existingBook);
  }

  public void deleteBook(Long id) {
    if (!bookRepository.existsById(id)) {
      throw new IllegalArgumentException("Book not found with id: " + id);
    }
    bookRepository.deleteById(id);
  }

  public void deleteByIsbn(String isbn) {
    if (!bookRepository.existsByIsbn(isbn)) {
      throw new IllegalArgumentException("Book not found with ISBN: " + isbn);
    }
    bookRepository.deleteByIsbn(isbn);
  }

  @Transactional(readOnly = true)
  public long countBooks() {
    return bookRepository.count();
  }

  @Transactional(readOnly = true)
  public long countByCategory(BookCategory category) {
    return bookRepository.countByCategory(category);
  }

  @Transactional(readOnly = true)
  public long count() {
    return bookRepository.count();
  }
}
