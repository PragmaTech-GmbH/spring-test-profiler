package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.List;

import digital.pragmatech.demo.controller.BookController;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvcTest creates a different, lighter Spring context than @SpringBootTest,
 * resulting in a separate context cache entry.
 */
@WebMvcTest(BookController.class)
class BookControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private BookService bookService;

  @Test
  void testGetAllBooks() throws Exception {
    Book book = new Book("Clean Code", "Robert C. Martin", "978-0132350884",
        new BigDecimal("34.99"), BookCategory.TECHNOLOGY);

    when(bookService.findAll()).thenReturn(List.of(book));

    mockMvc.perform(get("/api/books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Clean Code"));
  }

  @Test
  void testCreateBook() throws Exception {
    Book book = new Book("Clean Code", "Robert C. Martin", "978-0132350884",
        new BigDecimal("34.99"), BookCategory.TECHNOLOGY);
    book.setId(1L);

    when(bookService.createBook(any(Book.class))).thenReturn(book);

    mockMvc.perform(post("/api/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\",\"isbn\":\"978-0132350884\",\"price\":34.99,\"category\":\"TECHNOLOGY\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Clean Code"));
  }
}
