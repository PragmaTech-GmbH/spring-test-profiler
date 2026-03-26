package digital.pragmatech.demo.modulea;

import java.math.BigDecimal;

import digital.pragmatech.demo.modulea.entity.Book;
import digital.pragmatech.demo.modulea.entity.BookCategory;
import digital.pragmatech.demo.modulea.repository.BookRepository;
import digital.pragmatech.demo.modulea.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ModuleAIntegrationTest {

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private BookService bookService;

  @Test
  void testCreateAndFindBook() {
    Book book = new Book("Clean Architecture", "Robert C. Martin", "978-0134494166",
        new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    Book saved = bookService.createBook(book);

    assertThat(saved.getId()).isNotNull();
    assertThat(bookRepository.findByIsbn("978-0134494166")).isPresent();
  }

  @Test
  void testCountBooks() {
    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
        new BigDecimal("52.99"), BookCategory.TECHNOLOGY);

    bookService.createBook(book);

    assertThat(bookService.count()).isEqualTo(1);
  }
}
