package digital.pragmatech.demo.controller;

import java.util.Arrays;

import digital.pragmatech.demo.service.BookService;
import digital.pragmatech.springtestinsight.experiment.CustomTestExecutionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestExecutionListeners(value = CustomTestExecutionListener.class, mergeMode =  TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@WebMvcTest(BookController.class)
class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MockMvcTester testMvc;

  @Autowired
  private ApplicationContext context;

  @MockitoBean
  private BookService bookService;

  @Test
  void testGet() throws Exception {

//    Arrays.stream(context.getBeanDefinitionNames()).forEach(System.out::println);

    this.mockMvc
      .perform(get("/api/books")
        .header(ACCEPT, APPLICATION_JSON))
      .andExpect(status().is(200))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.size()", is(0)))
      .andDo(print());
  }
}
