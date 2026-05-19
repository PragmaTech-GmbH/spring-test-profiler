package digital.pragmatech.testing.extensions;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerFormatterTest {

  @Test
  void shouldFallbackToSimpleNameWhenNoExtensionSupportsCustomizer() {
    assertThat(
            ContextCustomizerFormatter.format(
                new PlainCustomizer(), List.of(new UnsupportedExtension())))
        .isEqualTo("PlainCustomizer");
  }

  @Test
  void shouldUseExtensionDescriptionWhenExtensionSupportsCustomizer() {
    assertThat(
            ContextCustomizerFormatter.format(
                new SyntheticContextCustomizer("dependencyOne", "dependency.one.property"),
                List.of(new SyntheticContextCustomizerExtension())))
        .isEqualTo(
            "SyntheticContextCustomizer[identifier=dependencyOne, configurationProperty=dependency.one.property]");
  }

  @Test
  void shouldFallbackToSimpleNameWhenExtensionFails() {
    assertThat(
            ContextCustomizerFormatter.format(
                new PlainCustomizer(), List.of(new FailingExtension())))
        .isEqualTo("PlainCustomizer");
  }

  private static final class PlainCustomizer {}

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
