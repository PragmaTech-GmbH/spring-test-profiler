package digital.pragmatech.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.ContextCacheUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class to access Spring's DefaultContextCache via reflection.
 * This allows us to get accurate cache statistics directly from Spring's internal cache.
 */
public class SpringContextCacheAccessor {

    private static final Logger logger = LoggerFactory.getLogger(SpringContextCacheAccessor.class);

    /**
     * Gets the Spring ContextCache instance from a TestContext.
     */
    public static ContextCache getContextCache(TestContext testContext) {
        try {
            // TestContext has a CacheAwareContextLoaderDelegate
            Field cacheAwareContextLoaderDelegateField = testContext.getClass().getDeclaredField("cacheAwareContextLoaderDelegate");
            cacheAwareContextLoaderDelegateField.setAccessible(true);
            Object cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegateField.get(testContext);

            // CacheAwareContextLoaderDelegate has a ContextCache
            Field contextCacheField = cacheAwareContextLoaderDelegate.getClass().getDeclaredField("contextCache");
            contextCacheField.setAccessible(true);
            return (ContextCache) contextCacheField.get(cacheAwareContextLoaderDelegate);
        } catch (Exception e) {
            logger.warn("Failed to access ContextCache via reflection", e);
            return null;
        }
    }

    /**
     * Gets the maximum cache size configured for Spring's context cache.
     */
    public static int getMaxCacheSize() {
        try {
            return ContextCacheUtils.retrieveMaxCacheSize();
        } catch (Exception e) {
            logger.debug("Could not retrieve max cache size", e);
            return 32; // Default Spring cache size
        }
    }

    /**
     * Gets cache statistics from the DefaultContextCache.
     */
    public static CacheStatistics getCacheStatistics(ContextCache contextCache) {
        if (contextCache == null) {
            return new CacheStatistics(0, 0, 0, getMaxCacheSize(), Collections.emptyList());
        }

        try {
            int size = 0;
            int hitCount = 0;
            int missCount = 0;
            List<String> contextKeys = new ArrayList<>();

            // Get size - this method should exist
            try {
                Method sizeMethod = contextCache.getClass().getMethod("size");
                size = (int) sizeMethod.invoke(contextCache);
            } catch (Exception e) {
                logger.debug("Could not get cache size", e);
            }

            // Try to get hit/miss counts - these may not exist in all versions
            try {
                Method getHitCountMethod = contextCache.getClass().getMethod("getHitCount");
                hitCount = (int) getHitCountMethod.invoke(contextCache);
            } catch (Exception e) {
                logger.debug("Could not get hit count (method may not exist)", e);
            }

            try {
                Method getMissCountMethod = contextCache.getClass().getMethod("getMissCount");
                missCount = (int) getMissCountMethod.invoke(contextCache);
            } catch (Exception e) {
                logger.debug("Could not get miss count (method may not exist)", e);
            }

            // Try to get context keys if available
            try {
                // DefaultContextCache has a private contextMap field
                Field contextMapField = contextCache.getClass().getDeclaredField("contextMap");
                contextMapField.setAccessible(true);
                Map<?, ?> contextMap = (Map<?, ?>) contextMapField.get(contextCache);

                for (Object key : contextMap.keySet()) {
                    contextKeys.add(key.toString());
                }
            } catch (Exception e) {
                logger.debug("Could not access context keys from cache", e);
            }

            return new CacheStatistics(size, hitCount, missCount, getMaxCacheSize(), contextKeys);
        } catch (Exception e) {
            logger.warn("Failed to get cache statistics via reflection", e);
            return new CacheStatistics(0, 0, 0, getMaxCacheSize(), Collections.emptyList());
        }
    }

    /**
     * Container for cache statistics.
     */
    public static class CacheStatistics {
        private final int size;
        private final int hitCount;
        private final int missCount;
        private final int maxSize;
        private final List<String> contextKeys;

        public CacheStatistics(int size, int hitCount, int missCount, int maxSize, List<String> contextKeys) {
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.maxSize = maxSize;
            this.contextKeys = new ArrayList<>(contextKeys);
        }

        public int getSize() {
            return size;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public int getHitCount() {
            return hitCount;
        }

        public int getMissCount() {
            return missCount;
        }

        public double getHitRatio() {
            int totalAccess = hitCount + missCount;
            return totalAccess > 0 ? (double) hitCount / totalAccess : 0.0;
        }

        public List<String> getContextKeys() {
            return Collections.unmodifiableList(contextKeys);
        }

        @Override
        public String toString() {
            return String.format("CacheStatistics{size=%d, maxSize=%d, hitCount=%d, missCount=%d, hitRatio=%.2f%%}",
                size, maxSize, hitCount, missCount, getHitRatio() * 100);
        }
    }
}
