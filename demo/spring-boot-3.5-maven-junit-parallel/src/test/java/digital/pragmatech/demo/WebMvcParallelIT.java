package digital.pragmatech.demo;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Parallel Integration test for Web MVC Layer - Set 3
 * Tests MVC endpoints with MockMvc - DIFFERENT CONTEXT (@AutoConfigureWebMvc creates different context)
 */
@SpringBootTest
@AutoConfigureWebMvc
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:mvctest;DB_CLOSE_DELAY=-1",
  "server.servlet.context-path=/api/v2"  // Different context path
})
@Execution(ExecutionMode.CONCURRENT)
public class WebMvcParallelIT {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Test
  void testCreateBookMvcParallel() throws Exception {
    // Simulate some processing time
    Thread.sleep(88);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    String bookJson = """
      {
          "title": "Building Microservices",
          "author": "Sam Newman",
          "isbn": "978-1491950357",
          "price": 52.99,
          "category": "TECHNOLOGY"
      }
      """;

    mockMvc.perform(post("/api/books")
        .contentType(MediaType.APPLICATION_JSON)
        .content(bookJson))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.title").value("Building Microservices"));
  }

  @Test
  void testGetAllBooksMvcParallel() throws Exception {
    // Simulate some processing time
    Thread.sleep(115);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void testGetBookCountMvcParallel() throws Exception {
    // Simulate some processing time
    Thread.sleep(65);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books/count")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  void testSearchBooksMvcParallel() throws Exception {
    // Simulate some processing time
    Thread.sleep(125);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books/search")
        .param("category", "TECHNOLOGY")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
