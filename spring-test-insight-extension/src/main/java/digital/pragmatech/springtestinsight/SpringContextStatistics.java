package digital.pragmatech.springtestinsight;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class SpringContextStatistics {
    private int contextLoads = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private final List<ContextLoadEvent> contextLoadEvents = new ArrayList<>();
    private final Map<String, Set<String>> cacheKeyToTestClasses = new HashMap<>();
    private final Map<String, CacheKeyInfo> cacheKeyInfoMap = new HashMap<>();
    
    public void recordContextLoad(String contextKey, Duration loadTime) {
        recordContextLoad(contextKey, loadTime, null);
    }
    
    public void recordContextLoad(String contextKey, Duration loadTime, String testClassName) {
        contextLoads++;
        cacheMisses++;
        contextLoadEvents.add(new ContextLoadEvent(contextKey, loadTime, Instant.now()));
        recordCacheKeyUsage(contextKey, testClassName, false);
    }
    
    public void recordCacheHit(String contextKey) {
        recordCacheHit(contextKey, null);
    }
    
    public void recordCacheHit(String contextKey, String testClassName) {
        cacheHits++;
        recordCacheKeyUsage(contextKey, testClassName, true);
    }
    
    private void recordCacheKeyUsage(String contextKey, String testClassName, boolean wasHit) {
        if (contextKey == null) return;
        
        cacheKeyToTestClasses.computeIfAbsent(contextKey, k -> new HashSet<>());
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
        return contextLoads;
    }
    
    public int getCacheHits() {
        return cacheHits;
    }
    
    public int getCacheMisses() {
        return cacheMisses;
    }
    
    public double getCacheHitRate() {
        int totalAccesses = cacheHits + cacheMisses;
        return totalAccesses > 0 ? (double) cacheHits / totalAccesses * 100 : 0;
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
        private int hits = 0;
        private int misses = 0;
        
        public CacheKeyInfo(String cacheKey) {
            this.cacheKey = cacheKey;
        }
        
        public void incrementHits() {
            hits++;
        }
        
        public void incrementMisses() {
            misses++;
        }
        
        public String getCacheKey() {
            return cacheKey;
        }
        
        public int getHits() {
            return hits;
        }
        
        public int getMisses() {
            return misses;
        }
        
        public int getTotalAccesses() {
            return hits + misses;
        }
        
        public double getHitRate() {
            int total = getTotalAccesses();
            return total > 0 ? (double) hits / total * 100 : 0;
        }
    }
}