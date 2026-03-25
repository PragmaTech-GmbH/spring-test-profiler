package digital.pragmatech.testing.util;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detects which Spring test annotation triggered the creation of a test context. Scans direct
 * annotations and one level of meta-annotations to catch composed annotations (e.g.,
 * {@code @MyIntegrationTest} meta-annotated with {@code @SpringBootTest}).
 */
public final class TestAnnotationDetector {

  private static final Map<String, String> KNOWN_ANNOTATIONS = new LinkedHashMap<>();

  static {
    KNOWN_ANNOTATIONS.put("org.springframework.boot.test.context.SpringBootTest", "SpringBootTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest", "WebMvcTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest", "DataJpaTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest", "DataMongoTest");
    KNOWN_ANNOTATIONS.put("org.springframework.boot.test.autoconfigure.jdbc.JdbcTest", "JdbcTest");
    KNOWN_ANNOTATIONS.put("org.springframework.boot.test.autoconfigure.json.JsonTest", "JsonTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.web.client.RestClientTest", "RestClientTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest", "WebFluxTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.boot.test.autoconfigure.graphql.GraphQlTest", "GraphQlTest");
    KNOWN_ANNOTATIONS.put(
        "org.springframework.test.context.ContextConfiguration", "ContextConfiguration");
  }

  private TestAnnotationDetector() {}

  /**
   * Detects the primary Spring test annotation type on the given test class.
   *
   * @param testClass the test class to inspect
   * @return a display label such as {@code "SpringBootTest"} or {@code "Unknown"}
   */
  public static String detectTestAnnotationType(Class<?> testClass) {
    if (testClass == null) {
      return "Unknown";
    }

    // Check direct annotations
    for (Annotation annotation : testClass.getAnnotations()) {
      String annotationName = annotation.annotationType().getName();
      String label = KNOWN_ANNOTATIONS.get(annotationName);
      if (label != null) {
        return label;
      }
    }

    // Check one level of meta-annotations (for composed annotations)
    for (Annotation annotation : testClass.getAnnotations()) {
      for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
        String metaAnnotationName = metaAnnotation.annotationType().getName();
        String label = KNOWN_ANNOTATIONS.get(metaAnnotationName);
        if (label != null) {
          return label;
        }
      }
    }

    return "Unknown";
  }
}
