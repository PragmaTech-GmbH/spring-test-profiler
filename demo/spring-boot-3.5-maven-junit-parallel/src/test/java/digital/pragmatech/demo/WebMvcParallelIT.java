package digital.pragmatech.demo;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parallel Integration test for Web MVC Layer - Set 3
 * Tests MVC endpoints with MockMvc - DIFFERENT CONTEXT (@AutoConfigureWebMvc creates different context)
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:mvctest;DB_CLOSE_DELAY=-1",
  "server.servlet.context-path=/api/v2"  // Different context path
})
@Execution(ExecutionMode.CONCURRENT)
class WebMvcParallelIT {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Test
  void testCreateBookMvcParallel() throws Exception {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(88, TimeUnit.MILLISECONDS)
      .until(() -> true);

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
    Awaitility.await()
      .pollDelay(115, TimeUnit.MILLISECONDS)
      .until(() -> true);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void testGetBookCountMvcParallel() throws Exception {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(65, TimeUnit.MILLISECONDS)
      .until(() -> true);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books/count")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  void testSearchBooksMvcParallel() throws Exception {
    // Simulate some processing time
    Awaitility.await()
      .pollDelay(125, TimeUnit.MILLISECONDS)
      .until(() -> true);

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc.perform(get("/api/books/search")
        .param("category", "TECHNOLOGY")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
