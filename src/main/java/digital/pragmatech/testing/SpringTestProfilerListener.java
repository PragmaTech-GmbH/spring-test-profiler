package digital.pragmatech.testing;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import digital.pragmatech.testing.reporting.html.TestExecutionReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Spring TestExecutionListener that tracks test execution and context cache usage.
 * This listener runs with highest precedence to capture context loading before Spring's own listeners.
 */
public class SpringTestProfilerListener extends AbstractTestExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(SpringTestProfilerListener.class);

  // Shared instances for tracking across all tests
  private static final TestExecutionTracker executionTracker = new TestExecutionTracker();
  private static final ContextCacheTracker contextCacheTracker = new ContextCacheTracker();
  private static final TestExecutionReporter reporter = new TestExecutionReporter();

  // Track current test class and method
  private final Map<TestContext, String> testClassNames = new ConcurrentHashMap<>();
  private final Map<TestContext, Instant> methodStartTimes = new ConcurrentHashMap<>();
  private final Map<TestContext, Instant> contextLoadStartTimes = new ConcurrentHashMap<>();

  // Static flag to ensure report is generated only once
  private static volatile boolean reportGenerated = false;
  private static volatile boolean shutdownHookRegistered = false;

  // Hold a reference to a TestContext so we can access the cache later
  private static final AtomicReference<TestContext> lastTestContext = new AtomicReference<>();

  @Override
  public int getOrder() {
    // Run with highest precedence to capture context loading early
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    String className = testClass.getName();

    logger.debug("Starting Spring Test Profiler for test class: {}", className);

    // Register shutdown hook once to generate report when JVM exits
    registerShutdownHook();

    // Start tracking if this is the first test class
    if (executionTracker.getTotalTestClasses() == 0) {
      executionTracker.startTracking();
    }

    // Record test class start
    testClassNames.put(testContext, className);
    executionTracker.recordTestClassStart(className);

    // Capture the TestContext reference for cache access
    lastTestContext.set(testContext);

    // Start timing context loading for this test class
    contextLoadStartTimes.put(testContext, Instant.now());

    // Extract and track context configuration
    TestContextBootstrapper bootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
    MergedContextConfiguration mergedConfig = bootstrapper.buildMergedContextConfiguration();

    // The cache key is the hashCode of the MergedContextConfiguration
    int cacheKey = mergedConfig.hashCode();

    // Track the association between context configuration and test class
    contextCacheTracker.recordTestClassForContext(mergedConfig, className);

    logger.info("Test class {} uses context cache key {}", className, cacheKey);
  }

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    // After test instance is prepared, the context should be loaded
    String className = testClassNames.get(testContext);
    Instant contextLoadEndTime = Instant.now();

    if (className != null) {
      try {
        Class<?> testClass = testContext.getTestClass();
        TestContextBootstrapper bootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
        MergedContextConfiguration mergedConfig = bootstrapper.buildMergedContextConfiguration();
        int cacheKey = mergedConfig.hashCode();

        // Calculate context loading time
        Instant contextLoadStartTime = contextLoadStartTimes.get(testContext);
        long contextLoadDurationMs = 0;
        if (contextLoadStartTime != null) {
          contextLoadDurationMs = java.time.Duration.between(contextLoadStartTime, contextLoadEndTime).toMillis();
        }

        // Try to get enhanced profile data from ApplicationContextInitializer
        ContextProfileData profileData = null;
        if (testContext.getApplicationContext() instanceof org.springframework.context.ConfigurableApplicationContext) {
          profileData = TimingTrackingApplicationContextInitializer
            .getContextProfileData((org.springframework.context.ConfigurableApplicationContext) testContext.getApplicationContext());
        }

        if (profileData != null) {
          logger.debug("Enhanced context profiling available for test class {} - Total time: {}ms, Memory: {}MB, Beans: {}",
            className, profileData.getTotalLoadTimeMs(), profileData.getMemoryUsedMB(),
            profileData.getBeanCreationMetrics() != null ? profileData.getBeanCreationMetrics().getTotalBeansCreated() : "unknown");
        }

        // Now check if this was a cache hit or miss
        // If the context was already tracked as created for another test, it's a hit
        Optional<ContextCacheEntry> entry = contextCacheTracker.getCacheEntry(mergedConfig);
        if (entry.isPresent() && entry.get().isCreated()) {
          contextCacheTracker.recordContextCacheHit(mergedConfig);
          logger.debug("Context cache hit for test class {} ({}ms)", className, contextLoadDurationMs);
        }
        else {
          contextCacheTracker.recordContextCreation(mergedConfig, contextLoadDurationMs);

          // Capture bean definitions for context complexity analysis

          String[] beanNames = testContext.getApplicationContext().getBeanDefinitionNames();
          contextCacheTracker.recordBeanDefinitions(mergedConfig, beanNames);
          logger.debug("New context created for test class {} with {} bean definitions ({}ms)",
            className, beanNames.length, contextLoadDurationMs);

        }
      }
      catch (Exception e) {
        logger.warn("Failed to track context loading for test class {}: {}", className, e.getMessage());
      }
      finally {
        // Clean up context load timing
        contextLoadStartTimes.remove(testContext);
      }
    }
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);
    if (className != null) {
      executionTracker.recordTestClassEnd(className);
      logger.debug("Completed Spring Test Profiler for test class: {}", className);
    }

    // Clean up
    testClassNames.remove(testContext);
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);
    String methodName = testContext.getTestMethod().getName();

    if (className != null) {
      executionTracker.recordTestMethodStart(className, methodName);
      methodStartTimes.put(testContext, Instant.now());

      // Record which test method uses this context
      Optional<MergedContextConfiguration> config = contextCacheTracker.getContextForTestClass(className);
      if (config.isPresent()) {
        contextCacheTracker.recordTestMethodForContext(config.get(), className, methodName);
      }
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);
    String methodName = testContext.getTestMethod().getName();

    if (className != null) {
      // Determine test status based on test exception
      TestStatus status = determineTestStatus(testContext);
      executionTracker.recordTestMethodEnd(className, methodName, status);

      // Clean up
      methodStartTimes.remove(testContext);
    }
  }

  private TestStatus determineTestStatus(TestContext testContext) {
    if (testContext.getTestException() != null) {
      Throwable exception = testContext.getTestException();

      // Check for test abortion (AssumptionViolatedException or similar)
      if (exception.getClass().getSimpleName().contains("AssumptionViolated") ||
        exception.getClass().getSimpleName().contains("TestAborted")) {
        return TestStatus.ABORTED;
      }

      return TestStatus.FAILED;
    }
    return TestStatus.PASSED;
  }

  /**
   * Registers a shutdown hook to ensure report generation when JVM exits.
   * This is called once when the first test class is processed.
   */
  private static void registerShutdownHook() {
    if (!shutdownHookRegistered) {
      synchronized (SpringTestProfilerListener.class) {
        if (!shutdownHookRegistered) {
          Runtime.getRuntime().addShutdownHook(new Thread(() -> generateReport(), "SpringTestProfilerReportGenerator"));
          shutdownHookRegistered = true;
          logger.debug("Registered shutdown hook for Spring Test Profiler report generation");
        }
      }
    }
  }

  /**
   * Called by the shutdown hook or manually to generate the final report.
   */
  public static void generateReport() {
    synchronized (SpringTestProfilerListener.class) {
      if (!reportGenerated) {
        logger.info("Generating Spring Test Profiler");
        executionTracker.stopTracking();

        // Get context cache statistics including our custom tracking
        SpringContextCacheAccessor.CacheStatistics springStats =
          getCacheStatistics();

        // Generate report with both execution and context cache data
        reporter.generateReport(executionTracker, springStats, contextCacheTracker);

        // Clear data
        contextCacheTracker.clear();
        reportGenerated = true;
      }
    }
  }

  /**
   * Gets the Spring ContextCache if available.
   */
  public static org.springframework.test.context.cache.ContextCache getContextCache() {
    TestContext context = lastTestContext.get();
    if (context != null) {
      return SpringContextCacheAccessor.getContextCache(context);
    }
    return null;
  }

  /**
   * Gets cache statistics from Spring's DefaultContextCache.
   */
  public static SpringContextCacheAccessor.CacheStatistics getCacheStatistics() {
    org.springframework.test.context.cache.ContextCache cache = getContextCache();
    return SpringContextCacheAccessor.getCacheStatistics(cache);
  }
}
