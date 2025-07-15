package digital.pragmatech.testing.context;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class SlowContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    try {
      Thread.sleep(2000); // Simulate a slow initialization
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
