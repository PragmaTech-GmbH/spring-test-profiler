package digital.pragmatech.demo.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class OtherService {

  public String doWork() {
    return "Other Service is working!";
  }
}
