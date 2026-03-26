package digital.pragmatech.demo.modulea;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.modulea.entity.Book;
import digital.pragmatech.demo.modulea.entity.BookCategory;
import digital.pragmatech.demo.modulea.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testFindByCategory() {
    Book scienceBook = new Book("Cosmos", "Carl Sagan", "978-0345539434",
        new BigDecimal("14.99"), BookCategory.SCIENCE);
    Book techBook = new Book("Clean Code", "Robert C. Martin", "978-0132350884",
        new BigDecimal("34.99"), BookCategory.TECHNOLOGY);

    entityManager.persist(scienceBook);
    entityManager.persist(techBook);
    entityManager.flush();

    List<Book> scienceBooks = bookRepository.findByCategory(BookCategory.SCIENCE);

    assertThat(scienceBooks).hasSize(1);
    assertThat(scienceBooks.get(0).getTitle()).isEqualTo("Cosmos");
  }

  @Test
  void testFindByIsbn() {
    Book book = new Book("Cosmos", "Carl Sagan", "978-0345539434",
        new BigDecimal("14.99"), BookCategory.SCIENCE);

    entityManager.persist(book);
    entityManager.flush();

    Optional<Book> found = bookRepository.findByIsbn("978-0345539434");

    assertThat(found).isPresent();
    assertThat(found.get().getAuthor()).isEqualTo("Carl Sagan");
  }
}
