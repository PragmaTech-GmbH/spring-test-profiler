package digital.pragmatech.testing.extensions;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Stores context customizer extensions discovered from Spring test application contexts. */
public final class ContextCustomizerExtensionRegistry {
  private static final ConcurrentMap<String, ContextCustomizerExtension> EXTENSIONS =
      new ConcurrentHashMap<>();

  private ContextCustomizerExtensionRegistry() {}

  public static void registerAll(Collection<? extends ContextCustomizerExtension> discovered) {
    if (discovered == null || discovered.isEmpty()) {
      return;
    }

    for (ContextCustomizerExtension extension : discovered) {
      if (extension != null) {
        EXTENSIONS.putIfAbsent(extension.getClass().getName(), extension);
      }
    }
  }

  public static List<ContextCustomizerExtension> getExtensions() {
    return EXTENSIONS.values().stream()
        .sorted(Comparator.comparing(extension -> extension.getClass().getName()))
        .toList();
  }

  public static void clear() {
    EXTENSIONS.clear();
  }
}
