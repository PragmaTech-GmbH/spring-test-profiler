package digital.pragmatech.testing.extensions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.util.ReflectionUtils;

/** Formats Spring context customizers with optional project-provided extensions. */
public final class ContextCustomizerFormatter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextCustomizerFormatter.class);

  private static final String DYNAMIC_PROPERTIES_CUSTOMIZER =
      "org.springframework.test.context.support.DynamicPropertiesContextCustomizer";

  private ContextCustomizerFormatter() {}

  public static List<String> formatAll(Set<ContextCustomizer> contextCustomizers) {
    List<ContextCustomizerExtension> extensions =
        ContextCustomizerExtensionRegistry.getExtensions();
    return Optional.ofNullable(contextCustomizers).orElse(Set.of()).stream()
        .filter(Objects::nonNull)
        .map(contextCustomizer -> format(contextCustomizer, extensions))
        .sorted()
        .toList();
  }

  static String format(
      ContextCustomizer contextCustomizer, List<ContextCustomizerExtension> extensions) {
    for (ContextCustomizerExtension extension : extensions) {
      try {
        if (extension.supports(contextCustomizer)) {
          Optional<String> description =
              Optional.ofNullable(extension.describe(contextCustomizer))
                  .filter(value -> !value.isBlank());
          if (description.isPresent()) {
            return description.get();
          }
        }
      } catch (RuntimeException ex) {
        LOGGER.debug(
            "Context customizer extension {} failed for {}: {}",
            extension.getClass().getName(),
            contextCustomizer.getClass().getName(),
            ex.getMessage());
      }
    }

    return describeBuiltIn(contextCustomizer);
  }

  /**
   * Describes Spring customizers whose equality depends on hidden state. Two test classes with
   * identical visible configuration still get separate cached contexts when this state differs, so
   * the state must show up in the report.
   */
  private static String describeBuiltIn(ContextCustomizer contextCustomizer) {
    String simpleName = contextCustomizer.getClass().getSimpleName();
    if (DYNAMIC_PROPERTIES_CUSTOMIZER.equals(contextCustomizer.getClass().getName())) {
      return describeDynamicProperties(contextCustomizer, simpleName);
    }
    return simpleName;
  }

  private static String describeDynamicProperties(
      ContextCustomizer contextCustomizer, String simpleName) {
    try {
      Field methodsField = contextCustomizer.getClass().getDeclaredField("methods");
      ReflectionUtils.makeAccessible(methodsField);
      Object fieldValue = methodsField.get(contextCustomizer);
      if (fieldValue instanceof Collection<?> methods && !methods.isEmpty()) {
        String methodNames =
            methods.stream()
                .filter(Method.class::isInstance)
                .map(Method.class::cast)
                .map(method -> method.getDeclaringClass().getSimpleName() + "#" + method.getName())
                .sorted()
                .collect(Collectors.joining(", "));
        return simpleName + "[@DynamicPropertySource=" + methodNames + "]";
      }
    } catch (ReflectiveOperationException | RuntimeException ex) {
      LOGGER.debug("Could not read dynamic property methods: {}", ex.getMessage());
    }
    return simpleName;
  }
}
