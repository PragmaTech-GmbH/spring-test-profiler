package digital.pragmatech.testing.plugins;

import java.util.List;
import java.util.Set;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.reporting.TemplateHelpers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerReportFormattingTest {

  @Test
  void shouldUsePluginDescriptionInConfigurationSummary() {
    MergedContextConfiguration config =
        createConfig(new SyntheticContextCustomizer("amazonS3Client", "amazons3.url"));

    ContextCacheEntry entry = new ContextCacheEntry(config);

    assertThat(entry.getConfigurationSummary())
        .containsEntry(
            "contextCustomizers",
            "SyntheticContextCustomizer[name=amazonS3Client, baseUrlProperty=amazons3.url]");
  }

  @Test
  void shouldUsePluginDescriptionInContextComparisonJson() {
    MergedContextConfiguration config =
        createConfig(new SyntheticContextCustomizer("seaweedFsClient", "seaweedfs.url"));
    ContextCacheTracker tracker = new ContextCacheTracker();
    tracker.recordTestClassForContext(config, "com.example.SeaweedFsClientIT");
    tracker.recordContextCreation(config, 100);

    String json = new TemplateHelpers.JsonHelper().contextStatisticsToJson(tracker);

    assertThat(json)
        .contains(
            "SyntheticContextCustomizer[name=seaweedFsClient, baseUrlProperty=seaweedfs.url]");
  }

  @Test
  void shouldReportDifferentValuesForSameCustomizerClassWithDifferentConfiguration() {
    ContextCustomizerFormatter formatter =
        new ContextCustomizerFormatter(List.of(new SyntheticContextCustomizerPlugin()));

    String amazonDescription =
        formatter.format(new SyntheticContextCustomizer("amazonS3Client", "amazons3.url"));
    String seaweedDescription =
        formatter.format(new SyntheticContextCustomizer("seaweedFsClient", "seaweedfs.url"));

    assertThat(amazonDescription).isNotEqualTo(seaweedDescription);
  }

  private MergedContextConfiguration createConfig(SyntheticContextCustomizer customizer) {
    return new MergedContextConfiguration(
        getClass(),
        new String[0],
        new Class<?>[] {getClass()},
        Set.of(),
        new String[] {"test"},
        new String[0],
        new String[0],
        Set.of(customizer),
        new StubContextLoader(),
        null,
        null);
  }

  private static final class StubContextLoader implements ContextLoader {
    @Override
    public String[] processLocations(Class<?> clazz, String... locations) {
      return locations;
    }

    @Override
    public ApplicationContext loadContext(String... locations) {
      return null;
    }
  }
}
