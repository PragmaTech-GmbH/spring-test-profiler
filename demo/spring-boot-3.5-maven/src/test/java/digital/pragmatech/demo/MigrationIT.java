package digital.pragmatech.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySources;

import static org.assertj.core.api.Assertions.assertThat;

// Spring Boot < 4.0
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=")
public class MigrationIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void shouldReturnSuccessfulHealthCheckRestTemplate() {
    var response = restTemplate
      .getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }
}
