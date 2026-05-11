package digital.pragmatech.testing.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Formats Spring context customizers with optional project-provided plugins. */
public final class ContextCustomizerFormatter {
  private static final Logger logger = LoggerFactory.getLogger(ContextCustomizerFormatter.class);
  private static final ContextCustomizerFormatter DEFAULT = new ContextCustomizerFormatter();

  private final List<ContextCustomizerPlugin> plugins;

  public ContextCustomizerFormatter() {
    this(loadPlugins());
  }

  ContextCustomizerFormatter(Collection<ContextCustomizerPlugin> plugins) {
    this.plugins = List.copyOf(plugins);
  }

  public static ContextCustomizerFormatter getDefault() {
    return DEFAULT;
  }

  public List<String> formatAll(Collection<?> contextCustomizers) {
    if (contextCustomizers == null || contextCustomizers.isEmpty()) {
      return List.of();
    }

    return contextCustomizers.stream().map(this::format).sorted().toList();
  }

  public String format(Object contextCustomizer) {
    if (contextCustomizer == null) {
      return "null";
    }

    for (ContextCustomizerPlugin plugin : plugins) {
      try {
        if (plugin.supports(contextCustomizer)) {
          String description = plugin.describe(contextCustomizer);
          if (description != null && !description.isBlank()) {
            return description;
          }
        }
      } catch (RuntimeException ex) {
        logger.debug(
            "Context customizer plugin {} failed for {}: {}",
            plugin.getClass().getName(),
            contextCustomizer.getClass().getName(),
            ex.getMessage());
      }
    }

    return contextCustomizer.getClass().getSimpleName();
  }

  private static List<ContextCustomizerPlugin> loadPlugins() {
    List<ContextCustomizerPlugin> loadedPlugins = new ArrayList<>();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ServiceLoader<ContextCustomizerPlugin> serviceLoader =
        classLoader != null
            ? ServiceLoader.load(ContextCustomizerPlugin.class, classLoader)
            : ServiceLoader.load(ContextCustomizerPlugin.class);

    for (ContextCustomizerPlugin plugin : serviceLoader) {
      loadedPlugins.add(plugin);
    }

    loadedPlugins.sort(Comparator.comparing(plugin -> plugin.getClass().getName()));
    return loadedPlugins;
  }
}
