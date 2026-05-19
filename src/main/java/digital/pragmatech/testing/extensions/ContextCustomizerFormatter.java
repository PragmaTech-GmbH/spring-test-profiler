package digital.pragmatech.testing.extensions;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Formats Spring context customizers with optional project-provided extensions. */
public final class ContextCustomizerFormatter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextCustomizerFormatter.class);

  private ContextCustomizerFormatter() {}

  public static List<String> formatAll(Collection<?> contextCustomizers) {
    return formatAll(contextCustomizers, ContextCustomizerExtensionRegistry.getExtensions());
  }

  static List<String> formatAll(
      Collection<?> contextCustomizers, Collection<ContextCustomizerExtension> extensions) {
    return Optional.ofNullable(contextCustomizers).orElse(List.of()).stream()
        .filter(contextCustomizer -> contextCustomizer != null)
        .map(contextCustomizer -> format(contextCustomizer, extensions))
        .sorted()
        .toList();
  }

  static String format(
      Object contextCustomizer, Collection<ContextCustomizerExtension> extensions) {
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

    return contextCustomizer.getClass().getSimpleName();
  }
}
