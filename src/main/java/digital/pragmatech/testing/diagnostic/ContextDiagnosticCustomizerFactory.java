package digital.pragmatech.testing.diagnostic;

import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Activates {@link ContextDiagnosticApplicationInitializer} for every Spring test context without
 * any user configuration. The factory is registered in this library's own
 * META-INF/spring.factories.
 */
public class ContextDiagnosticCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(
      Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
    return new DiagnosticContextCustomizer();
  }

  static final class DiagnosticContextCustomizer implements ContextCustomizer {

    @Override
    public void customizeContext(
        ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      new ContextDiagnosticApplicationInitializer().initialize(context);
    }

    // All instances are equal so the customizer never splits the context cache.
    @Override
    public boolean equals(Object that) {
      return that != null && getClass() == that.getClass();
    }

    @Override
    public int hashCode() {
      return DiagnosticContextCustomizer.class.hashCode();
    }
  }
}
