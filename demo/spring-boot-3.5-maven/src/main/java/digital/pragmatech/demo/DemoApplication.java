package digital.pragmatech.demo;

import java.util.Arrays;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

  private final ApplicationContext context;

  public DemoApplication(ApplicationContext context) {
    this.context = context;
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    String[] beanNames = context.getBeanDefinitionNames();
    System.out.println(Map.of(
      "count", beanNames.length,
      "beans", Arrays.asList(beanNames)
    ));
  }
}
