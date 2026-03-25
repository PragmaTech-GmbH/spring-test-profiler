package digital.pragmatech.demo;

import java.math.BigDecimal;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class BookServiceIT {

  @Autowired
  private BookService bookService;

  @Test
  void testCreateBook() {
    Book book = new Book("Spring in Action", "Craig Walls", "978-1617294945",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);

    Book created = bookService.createBook(book);
    assertNotNull(created);
  }

  @Test
  void testFindByAuthor() {
    Book book = new Book("Clean Code", "Robert Martin", "978-0132350884",
      new BigDecimal("45.99"), BookCategory.TECHNOLOGY);
    bookService.createBook(book);

    var found = bookService.findByAuthor("Robert Martin");
    assertEquals(1, found.size());
  }

  @Test
  void testCountByCategory() {
    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
      new BigDecimal("52.99"), BookCategory.TECHNOLOGY);
    bookService.createBook(book);

    long count = bookService.countByCategory(BookCategory.TECHNOLOGY);
    assertEquals(1, count);
  }
}
