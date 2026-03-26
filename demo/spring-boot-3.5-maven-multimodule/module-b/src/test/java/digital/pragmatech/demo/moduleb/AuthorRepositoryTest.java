package digital.pragmatech.demo.moduleb;

import java.util.List;

import digital.pragmatech.demo.moduleb.entity.Author;
import digital.pragmatech.demo.moduleb.repository.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private AuthorRepository authorRepository;

  @Test
  void testFindByNameContaining() {
    Author author1 = new Author("Robert C. Martin", "robert@cleancode.com");
    Author author2 = new Author("Martin Fowler", "martin@refactoring.com");

    entityManager.persist(author1);
    entityManager.persist(author2);
    entityManager.flush();

    List<Author> results = authorRepository.findByNameContainingIgnoreCase("martin");

    assertThat(results).hasSize(2);
  }

  @Test
  void testExistsByEmail() {
    Author author = new Author("Joshua Bloch", "joshua@effectivejava.com");

    entityManager.persist(author);
    entityManager.flush();

    assertThat(authorRepository.existsByEmail("joshua@effectivejava.com")).isTrue();
    assertThat(authorRepository.existsByEmail("unknown@example.com")).isFalse();
  }
}
