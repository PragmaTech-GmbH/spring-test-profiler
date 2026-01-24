package digital.pragmatech.demo.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype") // can now be replaced in tests with @MockitoBean/@SpyBean/@TestBean
public class OtherService {

  public String doWork() {
    return "Other Service is working!";
  }
}
