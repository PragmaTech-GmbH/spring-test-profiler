package digital.pragmatech.springtestinsight;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SpringContextStatistics {
    private int contextLoads = 0;
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private final List<ContextLoadEvent> contextLoadEvents = new ArrayList<>();
    
    public void recordContextLoad(String contextKey, Duration loadTime) {
        contextLoads++;
        cacheMisses++;
        contextLoadEvents.add(new ContextLoadEvent(contextKey, loadTime, Instant.now()));
    }
    
    public void recordCacheHit(String contextKey) {
        cacheHits++;
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
}