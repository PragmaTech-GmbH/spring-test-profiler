package digital.pragmatech.springtestinsight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Tracks context cache usage independently of Spring's internal cache.
 * This allows tracking more than Spring's default 32 context limit and provides
 * detailed information about which test classes use which contexts.
 */
public class ContextCacheTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextCacheTracker.class);
    
    // Map from cache key to context information
    private final Map<Integer, ContextCacheEntry> cacheEntries = new ConcurrentHashMap<>();
    
    // Map from test class name to cache key
    private final Map<String, Integer> testClassToCacheKey = new ConcurrentHashMap<>();
    
    // Track creation order for nearest context analysis
    private final List<Integer> contextCreationOrder = new CopyOnWriteArrayList<>();
    
    // Statistics
    private final AtomicInteger totalContextsCreated = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    /**
     * Records that a test class uses a specific context configuration.
     */
    public void recordTestClassForContext(int cacheKey, String testClassName, MergedContextConfiguration config) {
        testClassToCacheKey.put(testClassName, cacheKey);
        
        cacheEntries.computeIfAbsent(cacheKey, k -> {
            ContextCacheEntry entry = new ContextCacheEntry(cacheKey, config);
            logger.debug("Created new context cache entry for key: {}", cacheKey);
            return entry;
        }).addTestClass(testClassName);
    }
    
    /**
     * Records that a new context was created (cache miss).
     */
    public void recordContextCreation(int cacheKey) {
        ContextCacheEntry entry = cacheEntries.get(cacheKey);
        if (entry != null) {
            entry.recordCreation();
            contextCreationOrder.add(cacheKey);
            totalContextsCreated.incrementAndGet();
            cacheMisses.incrementAndGet();
            
            // Find nearest existing context if this is not the first one
            if (contextCreationOrder.size() > 1) {
                Integer nearestKey = findNearestCacheKey(cacheKey);
                if (nearestKey != null) {
                    entry.setNearestCacheKey(nearestKey);
                    logger.info("New context {} is most similar to existing context {}", 
                        cacheKey, nearestKey);
                }
            }
        }
    }
    
    /**
     * Records that a context was retrieved from cache (cache hit).
     */
    public void recordContextCacheHit(int cacheKey) {
        ContextCacheEntry entry = cacheEntries.get(cacheKey);
        if (entry != null) {
            entry.recordCacheHit();
            cacheHits.incrementAndGet();
        }
    }
    
    /**
     * Finds the most similar existing context to the given cache key.
     * This implementation uses configuration similarity scoring.
     */
    private Integer findNearestCacheKey(int cacheKey) {
        ContextCacheEntry targetEntry = cacheEntries.get(cacheKey);
        if (targetEntry == null || targetEntry.getConfiguration() == null) {
            return null;
        }
        
        MergedContextConfiguration targetConfig = targetEntry.getConfiguration();
        Integer nearestKey = null;
        int highestScore = 0;
        
        for (Map.Entry<Integer, ContextCacheEntry> entry : cacheEntries.entrySet()) {
            if (entry.getKey() == cacheKey) {
                continue; // Skip self
            }
            
            ContextCacheEntry candidate = entry.getValue();
            if (candidate.getConfiguration() == null || !candidate.isCreated()) {
                continue; // Skip entries without config or not yet created
            }
            
            int score = calculateSimilarityScore(targetConfig, candidate.getConfiguration());
            if (score > highestScore) {
                highestScore = score;
                nearestKey = entry.getKey();
            }
        }
        
        return nearestKey;
    }
    
    /**
     * Calculates a similarity score between two context configurations.
     * Higher score means more similar.
     */
    private int calculateSimilarityScore(MergedContextConfiguration config1, MergedContextConfiguration config2) {
        int score = 0;
        
        // Check configuration classes
        Set<Class<?>> classes1 = new HashSet<>(Arrays.asList(config1.getClasses()));
        Set<Class<?>> classes2 = new HashSet<>(Arrays.asList(config2.getClasses()));
        Set<Class<?>> commonClasses = new HashSet<>(classes1);
        commonClasses.retainAll(classes2);
        score += commonClasses.size() * 10; // Weight class matches heavily
        
        // Check active profiles
        Set<String> profiles1 = new HashSet<>(Arrays.asList(config1.getActiveProfiles()));
        Set<String> profiles2 = new HashSet<>(Arrays.asList(config2.getActiveProfiles()));
        if (profiles1.equals(profiles2)) {
            score += 5;
        }
        
        // Check context loader
        if (config1.getContextLoader() != null && config2.getContextLoader() != null &&
            config1.getContextLoader().getClass().equals(config2.getContextLoader().getClass())) {
            score += 3;
        }
        
        // Check property sources
        Set<String> props1 = new HashSet<>(Arrays.asList(config1.getPropertySourceProperties()));
        Set<String> props2 = new HashSet<>(Arrays.asList(config2.getPropertySourceProperties()));
        Set<String> commonProps = new HashSet<>(props1);
        commonProps.retainAll(props2);
        score += commonProps.size();
        
        // Check context initializers
        if (config1.getContextInitializerClasses().equals(config2.getContextInitializerClasses())) {
            score += 2;
        }
        
        return score;
    }
    
    /**
     * Gets all context cache entries.
     */
    public Collection<ContextCacheEntry> getAllEntries() {
        return Collections.unmodifiableCollection(cacheEntries.values());
    }
    
    /**
     * Gets the cache key for a specific test class.
     */
    public Optional<Integer> getCacheKeyForTestClass(String testClassName) {
        return Optional.ofNullable(testClassToCacheKey.get(testClassName));
    }
    
    /**
     * Gets a specific context cache entry.
     */
    public Optional<ContextCacheEntry> getCacheEntry(int cacheKey) {
        return Optional.ofNullable(cacheEntries.get(cacheKey));
    }
    
    /**
     * Gets the total number of unique contexts created.
     */
    public int getTotalContextsCreated() {
        return totalContextsCreated.get();
    }
    
    /**
     * Gets the total number of cache hits.
     */
    public int getCacheHits() {
        return cacheHits.get();
    }
    
    /**
     * Gets the total number of cache misses.
     */
    public int getCacheMisses() {
        return cacheMisses.get();
    }
    
    /**
     * Gets the cache hit ratio.
     */
    public double getCacheHitRatio() {
        int total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }
    
    /**
     * Clears all tracking data.
     */
    public void clear() {
        cacheEntries.clear();
        testClassToCacheKey.clear();
        contextCreationOrder.clear();
        totalContextsCreated.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * Entry representing a cached context configuration.
     */
    public static class ContextCacheEntry {
        private final int cacheKey;
        private final MergedContextConfiguration configuration;
        private final Set<String> testClasses = ConcurrentHashMap.newKeySet();
        private volatile boolean created = false;
        private volatile Instant creationTime;
        private final AtomicInteger hitCount = new AtomicInteger(0);
        private volatile Integer nearestCacheKey;
        
        public ContextCacheEntry(int cacheKey, MergedContextConfiguration configuration) {
            this.cacheKey = cacheKey;
            this.configuration = configuration;
        }
        
        public void addTestClass(String testClassName) {
            testClasses.add(testClassName);
        }
        
        public void recordCreation() {
            this.created = true;
            this.creationTime = Instant.now();
        }
        
        public void recordCacheHit() {
            hitCount.incrementAndGet();
        }
        
        public void setNearestCacheKey(Integer nearestKey) {
            this.nearestCacheKey = nearestKey;
        }
        
        public int getCacheKey() {
            return cacheKey;
        }
        
        public MergedContextConfiguration getConfiguration() {
            return configuration;
        }
        
        public Set<String> getTestClasses() {
            return Collections.unmodifiableSet(testClasses);
        }
        
        public boolean isCreated() {
            return created;
        }
        
        public Instant getCreationTime() {
            return creationTime;
        }
        
        public int getHitCount() {
            return hitCount.get();
        }
        
        public Optional<Integer> getNearestCacheKey() {
            return Optional.ofNullable(nearestCacheKey);
        }
        
        /**
         * Gets a summary of the configuration for reporting.
         */
        public Map<String, Object> getConfigurationSummary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            
            if (configuration != null) {
                // Configuration classes
                List<String> configClasses = Arrays.stream(configuration.getClasses())
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
                if (!configClasses.isEmpty()) {
                    summary.put("configurationClasses", configClasses);
                }
                
                // Active profiles
                if (configuration.getActiveProfiles().length > 0) {
                    summary.put("activeProfiles", Arrays.asList(configuration.getActiveProfiles()));
                }
                
                // Context loader
                if (configuration.getContextLoader() != null) {
                    summary.put("contextLoader", configuration.getContextLoader().getClass().getSimpleName());
                }
                
                // Property sources
                if (configuration.getPropertySourceProperties().length > 0) {
                    summary.put("properties", configuration.getPropertySourceProperties().length + " properties");
                }
                
                // Context initializers
                if (!configuration.getContextInitializerClasses().isEmpty()) {
                    List<String> initializers = configuration.getContextInitializerClasses().stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.toList());
                    summary.put("contextInitializers", initializers);
                }
            }
            
            return summary;
        }
    }
}