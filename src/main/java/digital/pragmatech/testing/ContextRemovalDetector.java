package digital.pragmatech.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.cache.ContextCache;

/**
 * Best-effort classification of why a context was removed from Spring's test context cache. Both
 * the @DirtiesContext path and the LRU eviction path close the context synchronously inside
 * DefaultContextCache.remove, so the calling stack at ContextClosedEvent time usually identifies
 * the cause. A cache-size heuristic is used as fallback.
 */
final class ContextRemovalDetector {

  private static final Logger logger = LoggerFactory.getLogger(ContextRemovalDetector.class);

  private ContextRemovalDetector() {}

  static ContextRemovalReason inferRemovalReason(ContextCache cacheOrNull) {
    try {
      ContextRemovalReason stackBasedReason =
          StackWalker.getInstance()
              .walk(
                  frames ->
                      frames
                          .map(ContextRemovalDetector::classifyFrame)
                          .filter(reason -> reason != ContextRemovalReason.UNKNOWN)
                          .findFirst()
                          .orElse(ContextRemovalReason.UNKNOWN));
      if (stackBasedReason != ContextRemovalReason.UNKNOWN) {
        return stackBasedReason;
      }

      // Fallback heuristic: after an LRU eviction the cache is full again; after a
      // @DirtiesContext removal it is normally below the maximum size.
      if (cacheOrNull != null) {
        int currentSize = SpringContextCacheAccessor.getCacheStatistics(cacheOrNull).size();
        if (currentSize >= SpringContextCacheAccessor.getMaxCacheSize()) {
          return ContextRemovalReason.CACHE_EVICTION;
        }
      }
    } catch (Exception e) {
      logger.debug("Failed to infer context removal reason", e);
    }
    return ContextRemovalReason.UNKNOWN;
  }

  private static ContextRemovalReason classifyFrame(StackWalker.StackFrame frame) {
    String className = frame.getClassName();
    String methodName = frame.getMethodName();
    if (className.contains("DirtiesContextTestExecutionListener")
        || (className.contains("TestContext")
            && methodName.contains("markApplicationContextDirty"))) {
      return ContextRemovalReason.DIRTIES_CONTEXT;
    }
    if (className.contains("DefaultContextCache$LruCache")
        || methodName.contains("removeEldestEntry")) {
      return ContextRemovalReason.CACHE_EVICTION;
    }
    return ContextRemovalReason.UNKNOWN;
  }
}
