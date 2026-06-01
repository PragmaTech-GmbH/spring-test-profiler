package digital.pragmatech.testing.extensions;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextCustomizer;

/** Formats Spring context customizers with optional project-provided extensions. */
public final class ContextCustomizerFormatter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextCustomizerFormatter.class);

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

    return contextCustomizer.getClass().getSimpleName();
  }
}
