package digital.pragmatech.testing.extensions;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Formats Spring context customizers with optional project-provided extensions. */
public final class ContextCustomizerFormatter {
  private static final Logger logger = LoggerFactory.getLogger(ContextCustomizerFormatter.class);

  private ContextCustomizerFormatter() {}

  public static List<String> formatAll(Collection<?> contextCustomizers) {
    return formatAll(contextCustomizers, ContextCustomizerExtensionRegistry.getExtensions());
  }

  static List<String> formatAll(
      Collection<?> contextCustomizers, Collection<ContextCustomizerExtension> extensions) {
    if (contextCustomizers == null || contextCustomizers.isEmpty()) {
      return List.of();
    }

    return contextCustomizers.stream()
        .map(contextCustomizer -> format(contextCustomizer, extensions))
        .sorted()
        .toList();
  }

  static String format(
      Object contextCustomizer, Collection<ContextCustomizerExtension> extensions) {
    if (contextCustomizer == null) {
      return "null";
    }

    for (ContextCustomizerExtension extension : extensions) {
      try {
        if (extension.supports(contextCustomizer)) {
          String description = extension.describe(contextCustomizer);
          if (description != null && !description.isBlank()) {
            return description;
          }
        }
      } catch (RuntimeException ex) {
        logger.debug(
            "Context customizer extension {} failed for {}: {}",
            extension.getClass().getName(),
            contextCustomizer.getClass().getName(),
            ex.getMessage());
      }
    }

    return contextCustomizer.getClass().getSimpleName();
  }
}
