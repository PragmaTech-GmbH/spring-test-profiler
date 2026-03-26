package digital.pragmatech.demo.moduleb;

import digital.pragmatech.demo.moduleb.entity.Author;
import digital.pragmatech.demo.moduleb.service.AuthorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ModuleBIntegrationTest {

  @Autowired
  private AuthorService authorService;

  @Test
  void testCreateAndFindAuthor() {
    Author author = new Author("Kent Beck", "kent@tdd.com");

    Author saved = authorService.createAuthor(author);

    assertThat(saved.getId()).isNotNull();
    assertThat(authorService.findByName("Kent")).hasSize(1);
  }

  @Test
  void testCountAuthors() {
    authorService.createAuthor(new Author("Uncle Bob", "bob@cleancode.com"));
    authorService.createAuthor(new Author("Eric Evans", "eric@ddd.com"));

    assertThat(authorService.count()).isEqualTo(2);
  }
}
