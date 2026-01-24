package digital.pragmatech.demo.pausing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthCheckSecondIT {

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
