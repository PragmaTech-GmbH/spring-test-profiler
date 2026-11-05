package digital.pragmatech.testing.extensions;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.MergedContextConfiguration;

import static digital.pragmatech.testing.extensions.ContextCustomizerFormatter.format;
import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerFormatterTest {

  @Test
  void shouldFallbackToSimpleNameWhenNoExtensionSupportsCustomizer() {
    ContextCustomizer contextCustomizer = new PlainCustomizer();
    assertThat(format(contextCustomizer, List.of(new UnsupportedExtension())))
        .isEqualTo("PlainCustomizer");
  }

  @Test
  void shouldUseExtensionDescriptionWhenExtensionSupportsCustomizer() {
    ContextCustomizer contextCustomizer =
        new SyntheticContextCustomizer("dependencyOne", "dependency.one.property");
    String expectedDescription =
        "SyntheticContextCustomizer[identifier=dependencyOne, configurationProperty=dependency.one.property]";

    assertThat(format(contextCustomizer, List.of(new SyntheticContextCustomizerExtension())))
        .isEqualTo(expectedDescription);
  }

  @Test
  void shouldFallbackToSimpleNameWhenExtensionFails() {
    ContextCustomizer contextCustomizer = new PlainCustomizer();
    assertThat(format(contextCustomizer, List.of(new FailingExtension())))
        .isEqualTo("PlainCustomizer");
  }

  @Test
  void shouldShowDeclaringClassOfDynamicPropertySourceMethod() {
    ContextCustomizer first = dynamicPropertiesCustomizerFor(FirstTest.class);
    ContextCustomizer second = dynamicPropertiesCustomizerFor(SecondTest.class);

    assertThat(format(first, List.of()))
        .isEqualTo(
            "DynamicPropertiesContextCustomizer[@DynamicPropertySource=FirstTest#properties]");
    assertThat(format(second, List.of())).isNotEqualTo(format(first, List.of()));
  }

  private static ContextCustomizer dynamicPropertiesCustomizerFor(Class<?> testClass) {
    return SpringFactoriesLoader.forDefaultResourceLocation(
            ContextCustomizerFormatterTest.class.getClassLoader())
        .load(ContextCustomizerFactory.class)
        .stream()
        .filter(factory -> factory.getClass().getSimpleName().startsWith("DynamicProperties"))
        .map(factory -> factory.createContextCustomizer(testClass, List.of()))
        .filter(Objects::nonNull)
        .filter(customizer -> customizer.getClass().getSimpleName().startsWith("DynamicProperties"))
        .findFirst()
        .orElseThrow();
  }

  static class FirstTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {}
  }

  static class SecondTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {}
  }

  private static final class PlainCustomizer implements ContextCustomizer {
    @Override
    public void customizeContext(
        ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {}
  }

  private static final class UnsupportedExtension implements ContextCustomizerExtension {
    @Override
    public boolean supports(Object contextCustomizer) {
      return false;
    }

    @Override
    public String describe(Object contextCustomizer) {
      return "unsupported";
    }
  }

  private static final class FailingExtension implements ContextCustomizerExtension {
    @Override
    public boolean supports(Object contextCustomizer) {
      throw new IllegalStateException("broken supports");
    }

    @Override
    public String describe(Object contextCustomizer) {
      return "broken";
    }
  }
}
