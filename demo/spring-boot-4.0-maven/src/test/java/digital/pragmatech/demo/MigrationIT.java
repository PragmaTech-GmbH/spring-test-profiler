package digital.pragmatech.demo;

import org.junit.jupiter.api.Test; // Junit 6 - yay
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient // consider as an alternative to the TestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MigrationIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private RestTestClient restClient;

  @Test
  void shouldReturnSuccessfulHealthCheckRestTemplate() {
    var response = restTemplate
      .getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  void shouldReturnSuccessfulHealthCheckRestClient() {
    restClient
      .get()
      .uri("/actuator/health")
      .exchange()
      .expectStatus()
      .is2xxSuccessful();
  }
}
