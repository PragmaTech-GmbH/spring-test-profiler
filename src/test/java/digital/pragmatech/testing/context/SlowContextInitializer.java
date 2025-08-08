package digital.pragmatech.testing.context;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class SlowContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    Awaitility.await()
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> true); // Simulate a slow initialization
  }
}
