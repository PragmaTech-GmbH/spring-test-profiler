package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import digital.pragmatech.demo.controller.BookController;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for BookController using @WebMvcTest slice
 * This creates a minimal Spring context with only web layer components
 * DISABLED: MockBean has concurrency issues during parallel execution with JUnit
 */
@Disabled("MockBean has concurrency issues during parallel execution with JUnit")
@WebMvcTest(BookController.class)
public class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

  @Test
  void testGetAllBooks() throws Exception {
    Book book1 = new Book("Spring in Action", "Craig Walls", "978-1617294945",
      new BigDecimal("39.99"), BookCategory.TECHNOLOGY);
    book1.setId(1L);
    Book book2 = new Book("Clean Code", "Robert Martin", "978-0132350884",
      new BigDecimal("45.99"), BookCategory.TECHNOLOGY);
    book2.setId(2L);

    when(bookService.findAll()).thenReturn(Arrays.asList(book1, book2));

    mockMvc.perform(get("/api/books"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].title").value("Spring in Action"))
      .andExpect(jsonPath("$[1].title").value("Clean Code"));
  }

  @Test
  void testGetBookById() throws Exception {
    Book book = new Book("Effective Java", "Joshua Bloch", "978-0134685991",
      new BigDecimal("52.99"), BookCategory.TECHNOLOGY);
    book.setId(1L);

    when(bookService.findById(1L)).thenReturn(Optional.of(book));

    mockMvc.perform(get("/api/books/1"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.title").value("Effective Java"))
      .andExpect(jsonPath("$.author").value("Joshua Bloch"));
  }

  @Test
  void testGetBookByIdNotFound() throws Exception {
    when(bookService.findById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/books/999"))
      .andExpect(status().isNotFound());
  }

  @Test
  void testCreateBook() throws Exception {
    Book inputBook = new Book("New Book", "New Author", "978-1234567890",
      new BigDecimal("29.99"), BookCategory.FICTION);
    Book createdBook = new Book("New Book", "New Author", "978-1234567890",
      new BigDecimal("29.99"), BookCategory.FICTION);
    createdBook.setId(1L);

    when(bookService.createBook(any(Book.class))).thenReturn(createdBook);

    mockMvc.perform(post("/api/books")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
              "title": "New Book",
              "author": "New Author",
              "isbn": "978-1234567890",
              "price": 29.99,
              "category": "FICTION"
          }
          """))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.title").value("New Book"));
  }

  @Test
  void testGetBookCount() throws Exception {
    when(bookService.countBooks()).thenReturn(42L);

    mockMvc.perform(get("/api/books/count"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").value(42));
  }

  @Test
  void testSearchBooksByAuthor() throws Exception {
    Book book = new Book("Spring Boot Guide", "John Doe", "978-1111111111",
      new BigDecimal("35.99"), BookCategory.TECHNOLOGY);
    book.setId(1L);

    when(bookService.findByAuthor("John Doe")).thenReturn(Arrays.asList(book));

    mockMvc.perform(get("/api/books/search?author=John Doe"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].author").value("John Doe"));
  }
}
