package digital.pragmatech.testing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Generates incrementing, human-readable context IDs for test contexts. Uses AtomicLong starting
 * from 0 to create sequential IDs like context-0, context-1, etc.
 */
public class ContextIdGenerator {
  private static final AtomicLong contextCounter = new AtomicLong(0);
  private static final ConcurrentHashMap<Integer, String> hashToIdMapping =
      new ConcurrentHashMap<>();

  /**
   * Gets or generates a human-readable context ID for the given configuration. Uses the
   * configuration's hashCode to ensure the same configuration always gets the same ID.
   *
   * @param configuration the merged context configuration
   * @return a human-readable context ID like "context-0", "context-1", etc.
   */
  public static String getContextId(MergedContextConfiguration configuration) {
    if (configuration == null) {
      return "context-unknown";
    }

    int hash = configuration.hashCode();
    return hashToIdMapping.computeIfAbsent(
        hash, h -> "context-" + contextCounter.getAndIncrement());
  }

  /** Gets the current counter value (for testing purposes). */
  static long getCurrentCounter() {
    return contextCounter.get();
  }

  /** Resets the counter and mapping (for testing purposes). */
  static void reset() {
    contextCounter.set(0);
    hashToIdMapping.clear();
  }
}
