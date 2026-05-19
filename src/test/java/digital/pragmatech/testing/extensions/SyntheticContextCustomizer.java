package digital.pragmatech.testing.extensions;

import java.util.Objects;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

final class SyntheticContextCustomizer implements ContextCustomizer {
  private final String identifier;
  private final String configurationProperty;

  SyntheticContextCustomizer(String identifier, String configurationProperty) {
    this.identifier = identifier;
    this.configurationProperty = configurationProperty;
  }

  String identifier() {
    return identifier;
  }

  String configurationProperty() {
    return configurationProperty;
  }

  @Override
  public void customizeContext(
      ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {}

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SyntheticContextCustomizer that)) {
      return false;
    }
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(configurationProperty, that.configurationProperty);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, configurationProperty);
  }
}
