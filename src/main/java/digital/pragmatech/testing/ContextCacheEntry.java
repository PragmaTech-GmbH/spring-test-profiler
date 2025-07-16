package digital.pragmatech.testing;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Entry representing a cached context configuration.
 */
public class ContextCacheEntry {
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
  private volatile long contextLoadTimeMs = 0;

  // Timeline tracking for future visualization
  private final List<Instant> accessTimes = new CopyOnWriteArrayList<>();

  public ContextCacheEntry(MergedContextConfiguration configuration) {
    this.configuration = configuration;
  }

  public void addTestClass(String testClassName) {
    testClasses.add(testClassName);
  }

  public void recordCreation() {
    recordCreation(0);
  }

  public void recordCreation(long loadTimeMs) {
    this.created = true;
    this.contextLoadTimeMs = loadTimeMs;
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
   *
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
   *
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
   *
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

  public long getContextLoadTimeMs() {
    return contextLoadTimeMs;
  }

  /**
   * Gets a summary of the configuration for reporting.
   */
  public Map<String, Object> getConfigurationSummary() {
    Map<String, Object> summary = new LinkedHashMap<>();

    if (configuration != null) {

      summary.put("configurationClasses", Arrays.stream(configuration.getClasses()).map(Class::getSimpleName).collect(Collectors.toList()));

      summary.put("activeProfiles", Arrays.asList(configuration.getActiveProfiles()));
      summary.put("contextLoader", configuration.getContextLoader().getClass().getSimpleName());

      summary.put("properties", configuration.getPropertySourceProperties().length + " properties");
      summary.put("parentContext", configuration.getParent());
      summary.put("contextCustomizers", configuration.getContextCustomizers());
      summary.put("locations", String.join(",", configuration.getLocations()));

      summary.put("contextInitializers", configuration.getContextInitializerClasses().stream()
        .map(Class::getSimpleName)
        .collect(Collectors.toList()));

      summary.put("beanDefinitionCount", beanDefinitionCount);
    }

    return summary;
  }
}
