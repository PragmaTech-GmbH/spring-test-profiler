package digital.pragmatech.testing.context;

import digital.pragmatech.testing.TimingTrackingApplicationContextInitializer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringContextCachingTest.FirstConfig.class, initializers = {TimingTrackingApplicationContextInitializer.class, SlowContextInitializer.class})
class SpringContextCachingTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testFirstContext() {
    assertNotNull(applicationContext);
  }

  @Configuration
  static class FirstConfig {
  }

  @Nested
  @ContextConfiguration(classes = SecondConfig.class, initializers = SlowContextInitializer.class)
  class DifferentContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testSecondContext() {
      // This should create a new context (cache miss)
      assertNotNull(applicationContext);
    }
  }

  @Configuration
  static class SecondConfig {

  }

  @Nested
  @ContextConfiguration(classes = FirstConfig.class)
  class SameContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testReuseFirstContext() {
      // This should reuse the first context (cache hit)
      assertNotNull(applicationContext);
    }
  }
}
