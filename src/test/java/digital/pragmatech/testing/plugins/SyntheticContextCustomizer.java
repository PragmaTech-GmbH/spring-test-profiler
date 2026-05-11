package digital.pragmatech.testing.plugins;

import java.util.Objects;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

final class SyntheticContextCustomizer implements ContextCustomizer {
  private final String name;
  private final String baseUrlProperty;

  SyntheticContextCustomizer(String name, String baseUrlProperty) {
    this.name = name;
    this.baseUrlProperty = baseUrlProperty;
  }

  String name() {
    return name;
  }

  String baseUrlProperty() {
    return baseUrlProperty;
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
    return Objects.equals(name, that.name) && Objects.equals(baseUrlProperty, that.baseUrlProperty);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, baseUrlProperty);
  }
}
