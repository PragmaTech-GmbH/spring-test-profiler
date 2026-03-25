package digital.pragmatech.testing.util;

import digital.pragmatech.testing.ContextCacheEntry;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class TestAnnotationDetectorTest {

  @Test
  void shouldDetectContextConfiguration() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(ContextConfigClass.class))
        .isEqualTo("ContextConfiguration");
  }

  @Test
  void shouldReturnUnknownForUnannotatedClass() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(PlainClass.class))
        .isEqualTo("Unknown");
  }

  @Test
  void shouldReturnUnknownForNullInput() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(null)).isEqualTo("Unknown");
  }

  // ContextCacheEntry annotation type tracking tests

  @Test
  void shouldStoreSingleAnnotationType() {
    ContextCacheEntry entry = new ContextCacheEntry(null);
    entry.addTestAnnotationType("SpringBootTest");

    assertThat(entry.getTestAnnotationTypes()).containsExactly("SpringBootTest");
    assertThat(entry.getPrimaryAnnotationType()).isEqualTo("SpringBootTest");
  }

  @Test
  void shouldReturnUnknownWhenNoAnnotationTypesAdded() {
    ContextCacheEntry entry = new ContextCacheEntry(null);

    assertThat(entry.getTestAnnotationTypes()).isEmpty();
    assertThat(entry.getPrimaryAnnotationType()).isEqualTo("Unknown");
  }

  @Test
  void shouldPreferNonUnknownAsPrimaryType() {
    ContextCacheEntry entry = new ContextCacheEntry(null);
    entry.addTestAnnotationType("Unknown");
    entry.addTestAnnotationType("WebMvcTest");

    assertThat(entry.getPrimaryAnnotationType()).isEqualTo("WebMvcTest");
  }

  @Test
  void shouldIgnoreNullAnnotationType() {
    ContextCacheEntry entry = new ContextCacheEntry(null);
    entry.addTestAnnotationType(null);

    assertThat(entry.getTestAnnotationTypes()).isEmpty();
  }

  // Test fixture classes

  @ContextConfiguration(classes = Object.class)
  static class ContextConfigClass {}

  static class PlainClass {}
}
