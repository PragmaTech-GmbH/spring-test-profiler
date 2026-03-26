package digital.pragmatech.testing.context;

import digital.pragmatech.testing.ContextProfileData;
import digital.pragmatech.testing.TimingTrackingApplicationContextInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the reported context load time is accurate. Uses a deliberately
 * slow bean (500ms delay during initialization) and checks that the profiler captures a load time
 * of at least that duration.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = ContextLoadTimeVerificationTest.SlowConfig.class,
    initializers = TimingTrackingApplicationContextInitializer.class)
class ContextLoadTimeVerificationTest {

  static final long SIMULATED_DELAY_MS = 500;

  @Autowired private ConfigurableApplicationContext applicationContext;

  @Test
  void contextLoadTimeShouldReflectActualLoadingDuration() {
    ContextProfileData profileData =
        TimingTrackingApplicationContextInitializer.getContextProfileData(applicationContext);

    assertThat(profileData).as("ContextProfileData should be available").isNotNull();
    assertThat(profileData.getTotalLoadTimeMs())
        .as("Load time should be at least %dms (the simulated bean init delay)", SIMULATED_DELAY_MS)
        .isGreaterThanOrEqualTo(SIMULATED_DELAY_MS);
  }

  @Configuration
  static class SlowConfig {

    @Bean
    public SlowInitBean slowInitBean() {
      return new SlowInitBean();
    }
  }

  static class SlowInitBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
      Thread.sleep(SIMULATED_DELAY_MS);
    }
  }
}
