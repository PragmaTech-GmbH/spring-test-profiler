package digital.pragmatech.demo.pausing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthCheckIT {

  @Autowired
  private RestTestClient restClient;

  @Test
  void shouldReturnSuccessfulHealthCheckRestClient() throws Exception {
    restClient
      .get()
      .uri("/actuator/health")
      .exchange()
      .expectStatus()
      .is2xxSuccessful();

    Thread.sleep(5_000); // Pausing to allow scheduled tasks to run
  }
}
