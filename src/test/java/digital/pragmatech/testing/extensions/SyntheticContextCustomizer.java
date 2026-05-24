package digital.pragmatech.testing.extensions;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

record SyntheticContextCustomizer(String identifier, String configurationProperty)
    implements ContextCustomizer {

  @Override
  public void customizeContext(
      ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {}
}
