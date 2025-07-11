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
    
    // Map from context configuration to list of test methods (format: "ClassName.methodName")
    private final Map<MergedContextConfiguration, List<String>> contextToTestMethods = new ConcurrentHashMap<>();
    
    // Map from context configuration to context information
    private final Map<MergedContextConfiguration, ContextCacheEntry> cacheEntries = new ConcurrentHashMap<>();
    
    // Map from test class name to context configuration
    private final Map<String, MergedContextConfiguration> testClassToContext = new ConcurrentHashMap<>();
    
    // Track creation order for nearest context analysis
    private final List<MergedContextConfiguration> contextCreationOrder = new CopyOnWriteArrayList<>();
    
    // Statistics
    private final AtomicInteger totalContextsCreated = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    /**
     * Records that a test class uses a specific context configuration.
     */
    public void recordTestClassForContext(MergedContextConfiguration config, String testClassName) {
        testClassToContext.put(testClassName, config);
        
        cacheEntries.computeIfAbsent(config, k -> {
            ContextCacheEntry entry = new ContextCacheEntry(config);
            logger.debug("Created new context cache entry for config: {}", config);
            return entry;
        }).addTestClass(testClassName);
    }
    
    /**
     * Records that a test method uses a specific context.
     */
    public void recordTestMethodForContext(MergedContextConfiguration config, String testClassName, String methodName) {
        String testMethodIdentifier = testClassName + "." + methodName;
        contextToTestMethods.computeIfAbsent(config, k -> new CopyOnWriteArrayList<>())
                            .add(testMethodIdentifier);
        logger.debug("Recorded test method {} for context config: {}", testMethodIdentifier, config);
    }
    
    /**
     * Records that a new context was created (cache miss).
     */
    public void recordContextCreation(MergedContextConfiguration config) {
        ContextCacheEntry entry = cacheEntries.get(config);
        if (entry != null) {
            entry.recordCreation();
            contextCreationOrder.add(config);
            totalContextsCreated.incrementAndGet();
            cacheMisses.incrementAndGet();
            
            // Find nearest existing context if this is not the first one
            if (contextCreationOrder.size() > 1) {
                MergedContextConfiguration nearestConfig = findNearestContext(config);
                if (nearestConfig != null) {
                    entry.setNearestContext(nearestConfig);
                    logger.info("New context {} is most similar to existing context {}", 
                        config, nearestConfig);
                }
            }
        }
    }
    
    /**
     * Records bean definitions for a context configuration.
     */
    public void recordBeanDefinitions(MergedContextConfiguration config, String[] beanNames) {
        ContextCacheEntry entry = cacheEntries.get(config);
        if (entry != null) {
            entry.setBeanDefinitions(beanNames);
            logger.debug("Recorded {} bean definitions for context: {}", beanNames.length, config);
        }
    }
    
    /**
     * Records that a context was retrieved from cache (cache hit).
     */
    public void recordContextCacheHit(MergedContextConfiguration config) {
        ContextCacheEntry entry = cacheEntries.get(config);
        if (entry != null) {
            entry.recordCacheHit();
            cacheHits.incrementAndGet();
        }
    }
    
    /**
     * Finds the most similar existing context to the given context configuration.
     * This implementation uses configuration similarity scoring.
     */
    private MergedContextConfiguration findNearestContext(MergedContextConfiguration targetConfig) {
        if (targetConfig == null) {
            return null;
        }
        
        MergedContextConfiguration nearestConfig = null;
        int highestScore = 0;
        
        for (Map.Entry<MergedContextConfiguration, ContextCacheEntry> entry : cacheEntries.entrySet()) {
            if (entry.getKey().equals(targetConfig)) {
                continue; // Skip self
            }
            
            ContextCacheEntry candidate = entry.getValue();
            if (!candidate.isCreated()) {
                continue; // Skip entries not yet created
            }
            
            int score = calculateSimilarityScore(targetConfig, entry.getKey());
            if (score > highestScore) {
                highestScore = score;
                nearestConfig = entry.getKey();
            }
        }
        
        return nearestConfig;
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
     * Gets the context configuration for a specific test class.
     */
    public Optional<MergedContextConfiguration> getContextForTestClass(String testClassName) {
        return Optional.ofNullable(testClassToContext.get(testClassName));
    }
    
    /**
     * Gets a specific context cache entry.
     */
    public Optional<ContextCacheEntry> getCacheEntry(MergedContextConfiguration config) {
        return Optional.ofNullable(cacheEntries.get(config));
    }
    
    /**
     * Gets the list of test methods that used a specific context configuration.
     */
    public List<String> getTestMethodsForContext(MergedContextConfiguration config) {
        return Collections.unmodifiableList(
            contextToTestMethods.getOrDefault(config, Collections.emptyList())
        );
    }
    
    /**
     * Gets all context configurations and their associated test methods.
     */
    public Map<MergedContextConfiguration, List<String>> getAllContextToTestMethods() {
        Map<MergedContextConfiguration, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<MergedContextConfiguration, List<String>> entry : contextToTestMethods.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return result;
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
     * Gets the earliest context creation time across all cached contexts.
     * This can be used as the start time for timeline visualization.
     */
    public Optional<Instant> getEarliestContextCreationTime() {
        return cacheEntries.values().stream()
            .filter(ContextCacheEntry::isCreated)
            .map(ContextCacheEntry::getCreationTime)
            .filter(Objects::nonNull)
            .min(Instant::compareTo);
    }
    
    /**
     * Gets the latest context access time across all cached contexts.
     * This can be used as the end time for timeline visualization.
     */
    public Optional<Instant> getLatestContextAccessTime() {
        return cacheEntries.values().stream()
            .map(ContextCacheEntry::getLastUsedTime)
            .filter(Objects::nonNull)
            .max(Instant::compareTo);
    }
    
    /**
     * Gets all context entries sorted by creation time for timeline visualization.
     */
    public List<ContextCacheEntry> getEntriesSortedByCreationTime() {
        return cacheEntries.values().stream()
            .filter(ContextCacheEntry::isCreated)
            .sorted((a, b) -> {
                Instant timeA = a.getCreationTime();
                Instant timeB = b.getCreationTime();
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeA.compareTo(timeB);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the total timeline span from first context creation to last context access.
     * @return Duration in milliseconds, or 0 if no contexts tracked
     */
    public long getTotalTimelineSpanInMillis() {
        Optional<Instant> earliest = getEarliestContextCreationTime();
        Optional<Instant> latest = getLatestContextAccessTime();
        
        if (earliest.isPresent() && latest.isPresent()) {
            return java.time.Duration.between(earliest.get(), latest.get()).toMillis();
        }
        return 0;
    }
    
    /**
     * Clears all tracking data.
     */
    public void clear() {
        contextToTestMethods.clear();
        cacheEntries.clear();
        testClassToContext.clear();
        contextCreationOrder.clear();
        totalContextsCreated.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * Entry representing a cached context configuration.
     */
    public static class ContextCacheEntry {
        private final MergedContextConfiguration configuration;
        private final Set<String> testClasses = ConcurrentHashMap.newKeySet();
        private volatile boolean created = false;
        private volatile Instant creationTime;
        private volatile Instant lastUsedTime;
        private volatile Instant firstUsedTime;
        private final AtomicInteger hitCount = new AtomicInteger(0);
        private volatile MergedContextConfiguration nearestContext;
        private volatile int beanDefinitionCount = 0;
        private volatile Set<String> beanDefinitionNames = ConcurrentHashMap.newKeySet();
        
        // Timeline tracking for future visualization
        private final List<Instant> accessTimes = new CopyOnWriteArrayList<>();
        
        public ContextCacheEntry(MergedContextConfiguration configuration) {
            this.configuration = configuration;
        }
        
        public void addTestClass(String testClassName) {
            testClasses.add(testClassName);
        }
        
        public void recordCreation() {
            this.created = true;
            Instant now = Instant.now();
            this.creationTime = now;
            this.firstUsedTime = now;
            this.lastUsedTime = now;
            this.accessTimes.add(now);
        }
        
        public void recordCacheHit() {
            hitCount.incrementAndGet();
            Instant now = Instant.now();
            this.lastUsedTime = now;
            this.accessTimes.add(now);
            
            // Set first used time if not already set (shouldn't happen, but defensive)
            if (this.firstUsedTime == null) {
                this.firstUsedTime = now;
            }
        }
        
        public void setNearestContext(MergedContextConfiguration nearestContext) {
            this.nearestContext = nearestContext;
        }
        
        public void setBeanDefinitions(String[] beanNames) {
            this.beanDefinitionCount = beanNames.length;
            this.beanDefinitionNames.clear();
            this.beanDefinitionNames.addAll(Arrays.asList(beanNames));
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
        
        public Instant getLastUsedTime() {
            return lastUsedTime;
        }
        
        public Instant getFirstUsedTime() {
            return firstUsedTime;
        }
        
        public int getHitCount() {
            return hitCount.get();
        }
        
        /**
         * Gets all access times for timeline visualization.
         * Returns an unmodifiable list of timestamps when this context was accessed.
         */
        public List<Instant> getAccessTimes() {
            return Collections.unmodifiableList(accessTimes);
        }
        
        /**
         * Calculates the age of this context from creation to now.
         * @return Duration in milliseconds, or -1 if not created yet
         */
        public long getAgeInMillis() {
            if (creationTime == null) {
                return -1;
            }
            return java.time.Duration.between(creationTime, Instant.now()).toMillis();
        }
        
        /**
         * Calculates how long this context has been active (from first to last use).
         * @return Duration in milliseconds, or 0 if used only once
         */
        public long getLifespanInMillis() {
            if (firstUsedTime == null || lastUsedTime == null) {
                return 0;
            }
            return java.time.Duration.between(firstUsedTime, lastUsedTime).toMillis();
        }
        
        /**
         * Calculates time since last use.
         * @return Duration in milliseconds since last access, or -1 if never used
         */
        public long getTimeSinceLastUseInMillis() {
            if (lastUsedTime == null) {
                return -1;
            }
            return java.time.Duration.between(lastUsedTime, Instant.now()).toMillis();
        }
        
        public Optional<MergedContextConfiguration> getNearestContext() {
            return Optional.ofNullable(nearestContext);
        }
        
        public int getBeanDefinitionCount() {
            return beanDefinitionCount;
        }
        
        public Set<String> getBeanDefinitionNames() {
            return Collections.unmodifiableSet(beanDefinitionNames);
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
                
                // Bean definitions count
                summary.put("beanDefinitionCount", beanDefinitionCount);
            }
            
            return summary;
        }
    }
}