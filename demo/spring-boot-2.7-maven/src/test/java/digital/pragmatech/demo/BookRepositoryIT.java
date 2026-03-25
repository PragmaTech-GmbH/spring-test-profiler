package digital.pragmatech.demo;

import java.math.BigDecimal;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class BookRepositoryIT {

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testSaveAndFind() {
    Book book = new Book("Domain-Driven Design", "Eric Evans", "978-0321125217",
      new BigDecimal("54.99"), BookCategory.TECHNOLOGY);

    Book saved = bookRepository.save(book);
    assertNotNull(saved.getId());

    var found = bookRepository.findByIsbn("978-0321125217");
    assertTrue(found.isPresent());
    assertEquals("Eric Evans", found.get().getAuthor());
  }

  @Test
  void testFindByCategory() {
    bookRepository.save(new Book("Book A", "Author A", "111-1111111111",
      new BigDecimal("10.00"), BookCategory.TECHNOLOGY));
    bookRepository.save(new Book("Book B", "Author B", "222-2222222222",
      new BigDecimal("20.00"), BookCategory.FICTION));

    var techBooks = bookRepository.findByCategory(BookCategory.TECHNOLOGY);
    assertEquals(1, techBooks.size());
  }
}
