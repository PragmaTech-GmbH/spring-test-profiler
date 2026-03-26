package digital.pragmatech.demo.modulea.service;

import java.util.List;

import digital.pragmatech.demo.modulea.entity.Book;
import digital.pragmatech.demo.modulea.repository.BookRepository;
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
  public List<Book> findAll() {
    return bookRepository.findAll();
  }

  @Transactional(readOnly = true)
  public long count() {
    return bookRepository.count();
  }
}
