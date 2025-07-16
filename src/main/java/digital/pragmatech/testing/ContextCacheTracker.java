package digital.pragmatech.testing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import digital.pragmatech.testing.optimization.ContextOptimizationOpportunity;
import digital.pragmatech.testing.reporting.ContextTimelineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.MergedContextConfiguration;

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
   * Records that a new context was created (cache miss) with timing information.
   */
  public void recordContextCreation(MergedContextConfiguration config, long loadTimeMs) {
    ContextCacheEntry entry = cacheEntries.get(config);
    if (entry != null) {
      entry.recordCreation(loadTimeMs);
      contextCreationOrder.add(config);
      totalContextsCreated.incrementAndGet();
      cacheMisses.incrementAndGet();

      // Find nearest existing context if this is not the first one
      if (contextCreationOrder.size() > 1) {
        MergedContextConfiguration nearestConfig = findNearestContext(config);
        if (nearestConfig != null) {
          entry.setNearestContext(nearestConfig);
          logger.info("New context {} is most similar to existing context {} (load time: {}ms)",
            config, nearestConfig, loadTimeMs);
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
   * Calculates optimization statistics based on context loading times and cache usage.
   */
  public OptimizationStatistics calculateOptimizationStatistics() {
    List<ContextCacheEntry> createdEntries = cacheEntries.values().stream()
      .filter(ContextCacheEntry::isCreated)
      .collect(Collectors.toList());

    if (createdEntries.isEmpty()) {
      return new OptimizationStatistics(0, 0, 0, 0, Collections.emptyList());
    }

    // Calculate total time spent creating contexts
    long totalContextCreationTimeMs = createdEntries.stream()
      .mapToLong(ContextCacheEntry::getContextLoadTimeMs)
      .sum();

    // Calculate potential time savings if contexts were harmonized
    long potentialTimeSavingsMs = calculatePotentialTimeSavings(createdEntries);

    // Calculate wasted time from duplicate context loads
    long wastedTimeMs = calculateWastedTime(createdEntries);

    // Find the slowest context loads
    List<ContextOptimizationOpportunity> opportunities = identifyOptimizationOpportunities(createdEntries);

    return new OptimizationStatistics(
      totalContextCreationTimeMs,
      potentialTimeSavingsMs,
      wastedTimeMs,
      createdEntries.size(),
      opportunities
    );
  }

  private long calculatePotentialTimeSavings(List<ContextCacheEntry> entries) {
    // For each context that could potentially be harmonized with a similar context,
    // calculate the time that could be saved
    long savings = 0;

    for (ContextCacheEntry entry : entries) {
      Optional<MergedContextConfiguration> nearest = entry.getNearestContext();
      if (nearest.isPresent()) {
        ContextCacheEntry nearestEntry = cacheEntries.get(nearest.get());
        if (nearestEntry != null && nearestEntry.isCreated()) {
          // Time that could be saved if this context was harmonized with the nearest one
          savings += entry.getContextLoadTimeMs();
        }
      }
    }

    return savings;
  }

  private long calculateWastedTime(List<ContextCacheEntry> entries) {
    // Calculate time wasted on slow context loads that could have been optimized
    return entries.stream()
      .filter(entry -> entry.getContextLoadTimeMs() > 1000) // Consider contexts taking >1s as potential waste
      .mapToLong(entry -> entry.getContextLoadTimeMs() - 1000) // Assume 1s is reasonable
      .sum();
  }

  private List<ContextOptimizationOpportunity> identifyOptimizationOpportunities(List<ContextCacheEntry> entries) {
    return entries.stream()
      .filter(entry -> entry.getContextLoadTimeMs() > 500) // Focus on contexts taking >500ms
      .sorted((a, b) -> Long.compare(b.getContextLoadTimeMs(), a.getContextLoadTimeMs())) // Sort by load time desc
      .limit(5) // Top 5 opportunities
      .map(entry -> {
        String recommendation = generateRecommendation(entry);
        return new ContextOptimizationOpportunity(
          entry.getTestClasses().iterator().next(), // Representative test class
          entry.getContextLoadTimeMs(),
          entry.getBeanDefinitionCount(),
          recommendation
        );
      })
      .collect(Collectors.toList());
  }

  private String generateRecommendation(ContextCacheEntry entry) {
    Optional<MergedContextConfiguration> nearest = entry.getNearestContext();
    if (nearest.isPresent()) {
      return "Consider harmonizing with similar context to save " + entry.getContextLoadTimeMs() + "ms";
    }
    else if (entry.getBeanDefinitionCount() > 100) {
      return "Large context (" + entry.getBeanDefinitionCount() + " beans) - consider using @TestConfiguration to reduce scope";
    }
    else if (entry.getContextLoadTimeMs() > 2000) {
      return "Slow context load (" + entry.getContextLoadTimeMs() + "ms) - review component scanning and auto-configuration";
    }
    else {
      return "Consider optimizing test setup to reduce context load time";
    }
  }

  /**
   * Gets timeline data for visualization of context lifecycle.
   * Returns data suitable for Chart.js timeline visualization showing actual test execution progression.
   */
  public TimelineData getTimelineData() {
    List<ContextCacheEntry> createdEntries = cacheEntries.values().stream()
      .filter(ContextCacheEntry::isCreated)
      .sorted((a, b) -> {
        Instant timeA = a.getCreationTime();
        Instant timeB = b.getCreationTime();
        if (timeA == null && timeB == null) {
          return 0;
        }
        if (timeA == null) {
          return 1;
        }
        if (timeB == null) {
          return -1;
        }
        return timeA.compareTo(timeB);
      })
      .collect(Collectors.toList());

    if (createdEntries.isEmpty()) {
      return new TimelineData(Collections.emptyList(), null, null, Collections.emptyList());
    }

    // Calculate timeline bounds
    Instant earliestCreation = createdEntries.get(0).getCreationTime();
    Instant latestAccess = createdEntries.stream()
      .map(ContextCacheEntry::getLastUsedTime)
      .filter(Objects::nonNull)
      .max(Instant::compareTo)
      .orElse(Instant.now());

    // Generate context events for timeline visualization
    List<ContextTimelineEvent> events = new ArrayList<>();
    List<String> contextColors = Arrays.asList(
      "#e74c3c", "#3498db", "#27ae60", "#f39c12", "#9b59b6",
      "#e67e22", "#1abc9c", "#34495e", "#e91e63", "#ff5722"
    );

    for (int i = 0; i < createdEntries.size(); i++) {
      ContextCacheEntry entry = createdEntries.get(i);

      String contextLabel = "Context " + (i + 1);
      if (!entry.getTestClasses().isEmpty()) {
        String firstTestClass = entry.getTestClasses().iterator().next();
        String simpleName = firstTestClass.substring(firstTestClass.lastIndexOf('.') + 1);
        contextLabel = simpleName;
      }

      String color = contextColors.get(i % contextColors.size());
      long creationTimeSeconds = java.time.Duration.between(earliestCreation, entry.getCreationTime()).toMillis() / 1000;

      events.add(new ContextTimelineEvent(
        contextLabel,
        color,
        creationTimeSeconds,
        entry.getContextLoadTimeMs(),
        entry.getTestClasses().size(),
        entry.getHitCount(),
        entry.getBeanDefinitionCount()
      ));
    }

    // Calculate timeline entries for the table
    List<TimelineEntry> timelineEntries = new ArrayList<>();
    for (int i = 0; i < createdEntries.size(); i++) {
      ContextCacheEntry entry = createdEntries.get(i);

      String contextLabel = events.get(i).contextName();
      long creationStartMs = java.time.Duration.between(earliestCreation, entry.getCreationTime()).toMillis();

      timelineEntries.add(new TimelineEntry(
        contextLabel,
        "Creation",
        creationStartMs,
        creationStartMs + entry.getContextLoadTimeMs(),
        events.get(i).color(),
        entry.getContextLoadTimeMs() + "ms load time",
        entry.getConfiguration().hashCode()
      ));
    }

    return new TimelineData(timelineEntries, earliestCreation, latestAccess, events);
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






}
