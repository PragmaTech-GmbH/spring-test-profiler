package digital.pragmatech.testing.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * Detects which Spring test annotation triggered the creation of a test context.
 *
 * <p>Annotations are matched by simple name (guarded by the {@code org.springframework.} package
 * prefix) so both the Spring Boot 3.x package layout ({@code
 * org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}) and the relocated Spring
 * Boot 4.x per-module packages ({@code org.springframework.boot.webmvc.test.autoconfigure
 * .WebMvcTest}) are recognized.
 *
 * <p>The full type hierarchy is scanned (superclasses, interfaces, and meta-annotations at
 * arbitrary depth), so annotations inherited from abstract base classes and custom composed
 * annotations are detected. When several known annotations are present (e.g. an inherited
 * {@code @ContextConfiguration} next to {@code @SpringBootTest}), the most specific one wins: slice
 * annotations first, then {@code SpringBootTest}, with the generic {@code ContextConfiguration} as
 * last-resort fallback.
 */
public final class TestAnnotationDetector {

  private static final Logger logger = LoggerFactory.getLogger(TestAnnotationDetector.class);

  private static final String UNKNOWN = "Unknown";

  private static final String SPRING_PACKAGE_PREFIX = "org.springframework.";

  /** Simple annotation names mapped to display labels; iteration order defines priority. */
  private static final Map<String, String> KNOWN_ANNOTATIONS = new LinkedHashMap<>();

  static {
    // Test slice annotations (most specific, highest priority)
    KNOWN_ANNOTATIONS.put("WebMvcTest", "WebMvcTest");
    KNOWN_ANNOTATIONS.put("WebFluxTest", "WebFluxTest");
    KNOWN_ANNOTATIONS.put("DataJpaTest", "DataJpaTest");
    KNOWN_ANNOTATIONS.put("DataJdbcTest", "DataJdbcTest");
    KNOWN_ANNOTATIONS.put("DataMongoTest", "DataMongoTest");
    KNOWN_ANNOTATIONS.put("DataRedisTest", "DataRedisTest");
    KNOWN_ANNOTATIONS.put("DataCassandraTest", "DataCassandraTest");
    KNOWN_ANNOTATIONS.put("DataElasticsearchTest", "DataElasticsearchTest");
    KNOWN_ANNOTATIONS.put("DataNeo4jTest", "DataNeo4jTest");
    KNOWN_ANNOTATIONS.put("DataR2dbcTest", "DataR2dbcTest");
    KNOWN_ANNOTATIONS.put("DataLdapTest", "DataLdapTest");
    KNOWN_ANNOTATIONS.put("JdbcTest", "JdbcTest");
    KNOWN_ANNOTATIONS.put("JooqTest", "JooqTest");
    KNOWN_ANNOTATIONS.put("JsonTest", "JsonTest");
    KNOWN_ANNOTATIONS.put("RestClientTest", "RestClientTest");
    KNOWN_ANNOTATIONS.put("WebServiceClientTest", "WebServiceClientTest");
    KNOWN_ANNOTATIONS.put("WebServiceServerTest", "WebServiceServerTest");
    KNOWN_ANNOTATIONS.put("GraphQlTest", "GraphQlTest");
    // Full application context
    KNOWN_ANNOTATIONS.put("SpringBootTest", "SpringBootTest");
    // Generic fallback (lowest priority)
    KNOWN_ANNOTATIONS.put("ContextConfiguration", "ContextConfiguration");
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
      return UNKNOWN;
    }

    String label = detectInTypeHierarchy(testClass);
    if (label != null) {
      return label;
    }

    // @Nested test classes may rely on the enclosing class annotations
    Class<?> enclosingClass = testClass.getEnclosingClass();
    if (enclosingClass != null) {
      return detectTestAnnotationType(enclosingClass);
    }

    return UNKNOWN;
  }

  private static String detectInTypeHierarchy(Class<?> testClass) {
    Set<String> presentSimpleNames;
    try {
      presentSimpleNames =
          MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
              .stream()
              .map(annotation -> annotation.getType())
              .filter(type -> type.getName().startsWith(SPRING_PACKAGE_PREFIX))
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());
    } catch (Throwable throwable) {
      logger.debug("Failed to inspect annotations of {}", testClass.getName(), throwable);
      return null;
    }

    for (Map.Entry<String, String> knownAnnotation : KNOWN_ANNOTATIONS.entrySet()) {
      if (presentSimpleNames.contains(knownAnnotation.getKey())) {
        return knownAnnotation.getValue();
      }
    }
    return null;
  }
}
