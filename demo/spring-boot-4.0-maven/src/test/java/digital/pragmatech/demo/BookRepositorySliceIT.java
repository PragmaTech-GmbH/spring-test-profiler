package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.Optional;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses the Spring Boot 4 relocated {@code @DataJpaTest} slice annotation so the profiler report
 * shows a DataJpaTest badge for this context (regression coverage for annotation detection).
 */
@DataJpaTest
class BookRepositorySliceIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testFindByIsbn() {
    Book book = new Book("Cosmos", "Carl Sagan", "978-0345539434",
        new BigDecimal("14.99"), BookCategory.SCIENCE);

    bookRepository.save(book);

    Optional<Book> found = bookRepository.findByIsbn("978-0345539434");

    assertThat(found).isPresent();
    assertThat(found.get().getAuthor()).isEqualTo("Carl Sagan");
  }
}
