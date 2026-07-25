package digital.pragmatech.demo;

import java.math.BigDecimal;
import java.util.List;

import digital.pragmatech.demo.controller.BookController;
import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses the Spring Boot 4 relocated {@code @WebMvcTest} slice annotation so the profiler report
 * shows a WebMvcTest badge for this context (regression coverage for annotation detection).
 */
@WebMvcTest(BookController.class)
class BookControllerSliceIT {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
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
  void testGetBookCount() throws Exception {
    when(bookService.count()).thenReturn(5L);

    mockMvc.perform(get("/api/books/count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(5));
  }
}
