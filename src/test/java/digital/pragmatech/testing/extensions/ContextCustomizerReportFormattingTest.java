package digital.pragmatech.testing.extensions;

import java.util.List;
import java.util.Set;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.reporting.TemplateHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerReportFormattingTest {

  @AfterEach
  void clearExtensionRegistry() {
    ContextCustomizerExtensionRegistry.clear();
  }

  @Test
  void shouldUseExtensionDescriptionInConfigurationSummary() {
    ContextCustomizerExtensionRegistry.registerAll(
        List.of(new SyntheticContextCustomizerExtension()));
    MergedContextConfiguration config =
        createConfig(new SyntheticContextCustomizer("dependencyOne", "dependency.one.property"));

    ContextCacheEntry entry = new ContextCacheEntry(config);

    assertThat(entry.getConfigurationSummary())
        .containsEntry(
            "contextCustomizers",
            "SyntheticContextCustomizer[identifier=dependencyOne, configurationProperty=dependency.one.property]");
  }

  @Test
  void shouldUseExtensionDescriptionInContextComparisonJson() {
    ContextCustomizerExtensionRegistry.registerAll(
        List.of(new SyntheticContextCustomizerExtension()));
    MergedContextConfiguration config =
        createConfig(new SyntheticContextCustomizer("dependencyTwo", "dependency.two.property"));
    ContextCacheTracker tracker = new ContextCacheTracker();
    tracker.recordTestClassForContext(config, "com.example.SecondContextTest");
    tracker.recordContextCreation(config, 100);

    String json = new TemplateHelpers.JsonHelper().contextStatisticsToJson(tracker);

    assertThat(json)
        .contains(
            "SyntheticContextCustomizer[identifier=dependencyTwo, configurationProperty=dependency.two.property]");
  }

  @Test
  void shouldReportDifferentValuesForSameCustomizerClassWithDifferentConfiguration() {
    List<ContextCustomizerExtension> extensions =
        List.of(new SyntheticContextCustomizerExtension());

    String firstDescription =
        ContextCustomizerFormatter.format(
            new SyntheticContextCustomizer("dependencyOne", "dependency.one.property"), extensions);
    String secondDescription =
        ContextCustomizerFormatter.format(
            new SyntheticContextCustomizer("dependencyTwo", "dependency.two.property"), extensions);

    assertThat(firstDescription).isNotEqualTo(secondDescription);
  }

  private MergedContextConfiguration createConfig(SyntheticContextCustomizer customizer) {
    return new MergedContextConfiguration(
        getClass(),
        null,
        new Class<?>[] {getClass()},
        null,
        new String[0],
        new String[0],
        null,
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
