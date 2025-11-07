package digital.pragmatech.testing.diagnostic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextDiagnosticApplicationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final Logger LOG =
      LoggerFactory.getLogger(ContextDiagnosticApplicationInitializer.class);

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    ContextDiagnostic contextDiagnostic = ContextDiagnostic.started();

    applicationContext.addApplicationListener(
        event -> {
          if (event instanceof ApplicationReadyEvent readyEvent) {
            if (readyEvent.getApplicationContext().getParent() == null
                && !applicationContext.getBeanFactory().containsSingleton("contextDiagnostic")) {
              ContextDiagnostic completedDiagnostic = contextDiagnostic.completed();
              applicationContext
                  .getBeanFactory()
                  .registerSingleton("contextDiagnostic", completedDiagnostic);
              LOG.debug(
                  "Context Diagnostic Completed: {} in {}",
                  completedDiagnostic,
                  completedDiagnostic.getContextLoadDuration() / 1000);
            }
          }
        });
  }

  @Override
  public boolean equals(Object that) {
    // avoid double registration of the same bean when listener was discovered via
    // spring.factories
    return getClass() == that.getClass();
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
