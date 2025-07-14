package digital.pragmatech.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SpringContextStatistics {
    private final AtomicInteger contextLoads = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final List<ContextLoadEvent> contextLoadEvents = new CopyOnWriteArrayList<>();
    private final Map<String, Set<String>> cacheKeyToTestClasses = new ConcurrentHashMap<>();
    private final Map<String, CacheKeyInfo> cacheKeyInfoMap = new ConcurrentHashMap<>();

    public void recordContextLoad(String contextKey, Duration loadTime) {
        recordContextLoad(contextKey, loadTime, null);
    }

    public void recordContextLoad(String contextKey, Duration loadTime, String testClassName) {
        contextLoads.incrementAndGet();
        cacheMisses.incrementAndGet();
        contextLoadEvents.add(new ContextLoadEvent(contextKey, loadTime, Instant.now()));
        recordCacheKeyUsage(contextKey, testClassName, false);
    }

    public void recordCacheHit(String contextKey) {
        recordCacheHit(contextKey, null);
    }

    public void recordCacheHit(String contextKey, String testClassName) {
        cacheHits.incrementAndGet();
        recordCacheKeyUsage(contextKey, testClassName, true);
    }

    private synchronized void recordCacheKeyUsage(String contextKey, String testClassName, boolean wasHit) {
        if (contextKey == null) return;

        cacheKeyToTestClasses.computeIfAbsent(contextKey, k -> ConcurrentHashMap.newKeySet());
        if (testClassName != null) {
            cacheKeyToTestClasses.get(contextKey).add(testClassName);
        }

        CacheKeyInfo info = cacheKeyInfoMap.computeIfAbsent(contextKey, k -> new CacheKeyInfo(k));
        if (wasHit) {
            info.incrementHits();
        } else {
            info.incrementMisses();
        }
    }

    public int getContextLoads() {
        return contextLoads.get();
    }

    public int getCacheHits() {
        return cacheHits.get();
    }

    public int getCacheMisses() {
        return cacheMisses.get();
    }

    public double getCacheHitRate() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int totalAccesses = hits + misses;
        return totalAccesses > 0 ? (double) hits / totalAccesses * 100 : 0;
    }

    public List<ContextLoadEvent> getContextLoadEvents() {
        return new ArrayList<>(contextLoadEvents);
    }

    public Duration getTotalContextLoadTime() {
        return contextLoadEvents.stream()
            .map(ContextLoadEvent::getLoadTime)
            .reduce(Duration.ZERO, Duration::plus);
    }

    public Map<String, Set<String>> getCacheKeyToTestClasses() {
        return new HashMap<>(cacheKeyToTestClasses);
    }

    public Map<String, CacheKeyInfo> getCacheKeyInfoMap() {
        return new HashMap<>(cacheKeyInfoMap);
    }

    public static class ContextLoadEvent {
        private final String contextKey;
        private final Duration loadTime;
        private final Instant timestamp;

        public ContextLoadEvent(String contextKey, Duration loadTime, Instant timestamp) {
            this.contextKey = contextKey;
            this.loadTime = loadTime;
            this.timestamp = timestamp;
        }

        public String getContextKey() {
            return contextKey;
        }

        public Duration getLoadTime() {
            return loadTime;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    public static class CacheKeyInfo {
        private final String cacheKey;
        private final AtomicInteger hits = new AtomicInteger(0);
        private final AtomicInteger misses = new AtomicInteger(0);

        public CacheKeyInfo(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        public void incrementHits() {
            hits.incrementAndGet();
        }

        public void incrementMisses() {
            misses.incrementAndGet();
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public int getHits() {
            return hits.get();
        }

        public int getMisses() {
            return misses.get();
        }

        public int getTotalAccesses() {
            return hits.get() + misses.get();
        }

        public double getHitRate() {
            int h = hits.get();
            int m = misses.get();
            int total = h + m;
            return total > 0 ? (double) h / total * 100 : 0;
        }
    }
}
