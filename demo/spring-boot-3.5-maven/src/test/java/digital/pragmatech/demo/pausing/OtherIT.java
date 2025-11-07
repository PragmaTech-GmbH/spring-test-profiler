package digital.pragmatech.demo.pausing;

import digital.pragmatech.demo.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=")
public class OtherIT {

  @MockitoBean
  private BookService bookService;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void shouldReturnSuccessfulHealthCheckRestTemplate() throws Exception {
    var response = restTemplate
      .getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);

    Thread.sleep(5_000); // Pausing to allow scheduled tasks to run
  }
}
