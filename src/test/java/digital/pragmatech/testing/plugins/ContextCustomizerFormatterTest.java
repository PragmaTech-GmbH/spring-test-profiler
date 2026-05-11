package digital.pragmatech.testing.plugins;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerFormatterTest {

  @Test
  void shouldFallbackToSimpleNameWhenNoPluginSupportsCustomizer() {
    ContextCustomizerFormatter formatter =
        new ContextCustomizerFormatter(List.of(new UnsupportedPlugin()));

    assertThat(formatter.format(new PlainCustomizer())).isEqualTo("PlainCustomizer");
  }

  @Test
  void shouldUsePluginDescriptionWhenPluginSupportsCustomizer() {
    ContextCustomizerFormatter formatter =
        new ContextCustomizerFormatter(List.of(new SyntheticContextCustomizerPlugin()));

    assertThat(formatter.format(new SyntheticContextCustomizer("amazonS3Client", "amazons3.url")))
        .isEqualTo("SyntheticContextCustomizer[name=amazonS3Client, baseUrlProperty=amazons3.url]");
  }

  @Test
  void shouldFallbackToSimpleNameWhenPluginFails() {
    ContextCustomizerFormatter formatter =
        new ContextCustomizerFormatter(List.of(new FailingPlugin()));

    assertThat(formatter.format(new PlainCustomizer())).isEqualTo("PlainCustomizer");
  }

  private static final class PlainCustomizer {}

  private static final class UnsupportedPlugin implements ContextCustomizerPlugin {
    @Override
    public boolean supports(Object contextCustomizer) {
      return false;
    }

    @Override
    public String describe(Object contextCustomizer) {
      return "unsupported";
    }
  }

  private static final class FailingPlugin implements ContextCustomizerPlugin {
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
