package digital.pragmatech.demo.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SlowDown implements CommandLineRunner {

  @Override
  public void run(String... args) throws Exception {
    try {
      Thread.sleep(5000);
      System.out.println("ApplicationContext initialized after delay.");
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
