package digital.pragmatech.testing.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import digital.pragmatech.testing.ContextCacheEntry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class TestAnnotationDetectorTest {

  // Spring Boot 3.x package layout

  @Test
  void shouldDetectBoot3WebMvcTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(Boot3WebMvcClass.class))
        .isEqualTo("WebMvcTest");
  }

  // Spring Boot 4.x relocated per-module packages

  @Test
  void shouldDetectBoot4WebMvcTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(Boot4WebMvcClass.class))
        .isEqualTo("WebMvcTest");
  }

  @Test
  void shouldDetectBoot4DataJpaTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(Boot4DataJpaClass.class))
        .isEqualTo("DataJpaTest");
  }

  @Test
  void shouldDetectBoot4RestClientTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(Boot4RestClientClass.class))
        .isEqualTo("RestClientTest");
  }

  @Test
  void shouldDetectBoot4DataJdbcTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(Boot4DataJdbcClass.class))
        .isEqualTo("DataJdbcTest");
  }

  @Test
  void shouldDetectSpringBootTest() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(SpringBootTestClass.class))
        .isEqualTo("SpringBootTest");
  }

  // Priority: SpringBootTest must win over the generic ContextConfiguration

  @Test
  void shouldPreferSpringBootTestWhenContextConfigurationIsDeclaredFirst() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(ContextConfigBeforeSpringBoot.class))
        .isEqualTo("SpringBootTest");
  }

  @Test
  void shouldPreferSpringBootTestWhenContextConfigurationIsDeclaredLast() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(SpringBootBeforeContextConfig.class))
        .isEqualTo("SpringBootTest");
  }

  // Inheritance from abstract base classes

  @Test
  void shouldDetectSpringBootTestInheritedFromAbstractBaseClass() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(ConcreteIntegrationTest.class))
        .isEqualTo("SpringBootTest");
  }

  @Test
  void shouldDetectSliceAnnotationInheritedFromAbstractBaseClass() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(ConcreteWebMvcTest.class))
        .isEqualTo("WebMvcTest");
  }

  // Composed / meta-annotations

  @Test
  void shouldDetectComposedAnnotation() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(ComposedAnnotationClass.class))
        .isEqualTo("SpringBootTest");
  }

  @Test
  void shouldDetectDeeplyComposedAnnotation() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(DeeplyComposedAnnotationClass.class))
        .isEqualTo("SpringBootTest");
  }

  // @Nested test classes

  @Test
  void shouldDetectAnnotationFromEnclosingClassForNestedTests() {
    assertThat(
            TestAnnotationDetector.detectTestAnnotationType(
                SpringBootTestClass.NestedTestClass.class))
        .isEqualTo("SpringBootTest");
  }

  // Package guard

  @Test
  void shouldNotDetectNonSpringAnnotationWithKnownSimpleName() {
    assertThat(TestAnnotationDetector.detectTestAnnotationType(DecoyWebMvcClass.class))
        .isEqualTo("Unknown");
  }

  // Existing behavior

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

  @org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
  static class Boot3WebMvcClass {}

  @org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
  static class Boot4WebMvcClass {}

  @org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
  static class Boot4DataJpaClass {}

  @org.springframework.boot.restclient.test.autoconfigure.RestClientTest
  static class Boot4RestClientClass {}

  @org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
  static class Boot4DataJdbcClass {}

  @SpringBootTest
  static class SpringBootTestClass {

    class NestedTestClass {}
  }

  @ContextConfiguration(classes = Object.class)
  @SpringBootTest
  static class ContextConfigBeforeSpringBoot {}

  @SpringBootTest
  @ContextConfiguration(classes = Object.class)
  static class SpringBootBeforeContextConfig {}

  @ContextConfiguration(classes = Object.class)
  @SpringBootTest
  abstract static class AbstractIntegrationTest {}

  static class ConcreteIntegrationTest extends AbstractIntegrationTest {}

  @org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
  abstract static class AbstractWebMvcTest {}

  static class ConcreteWebMvcTest extends AbstractWebMvcTest {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @SpringBootTest
  @interface MyIntegrationTest {}

  @MyIntegrationTest
  static class ComposedAnnotationClass {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @MyIntegrationTest
  @interface OuterComposedTest {}

  @OuterComposedTest
  static class DeeplyComposedAnnotationClass {}

  @digital.pragmatech.testing.util.fixture.WebMvcTest
  static class DecoyWebMvcClass {}

  @ContextConfiguration(classes = Object.class)
  static class ContextConfigClass {}

  static class PlainClass {}
}
