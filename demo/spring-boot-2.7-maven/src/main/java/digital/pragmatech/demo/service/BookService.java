package digital.pragmatech.demo.service;

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
  public List<Book> findByAuthor(String author) {
    return bookRepository.findByAuthorContainingIgnoreCase(author);
  }

  @Transactional(readOnly = true)
  public long countByCategory(BookCategory category) {
    return bookRepository.countByCategory(category);
  }

  @Transactional(readOnly = true)
  public List<Book> findAll() {
    return bookRepository.findAll();
  }
}
