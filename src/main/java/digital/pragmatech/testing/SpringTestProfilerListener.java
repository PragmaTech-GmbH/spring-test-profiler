package digital.pragmatech.testing;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import digital.pragmatech.testing.diagnostic.ContextDiagnostic;
import digital.pragmatech.testing.extensions.ContextCustomizerExtension;
import digital.pragmatech.testing.extensions.ContextCustomizerExtensionRegistry;
import digital.pragmatech.testing.reporting.html.TestExecutionReporter;
import digital.pragmatech.testing.util.TestAnnotationDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Spring TestExecutionListener that tracks test execution and context cache usage. This listener
 * runs with highest precedence to capture context loading before Spring's own listeners.
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

  // Once report generation started, context close events must no longer mutate the tracker
  private static volatile boolean reportGenerationStarted = false;

  // Context instances that already have a ContextClosedEvent listener registered
  private static final Set<ApplicationContext> closeListenerRegistered =
      ConcurrentHashMap.newKeySet();

  // Hold a reference to a TestContext so we can access the cache later
  private static final AtomicReference<TestContext> lastTestContext = new AtomicReference<>();

  @Override
  public int getOrder() {
    // Run with highest precedence to capture context loading early
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void beforeTestClass(@NonNull TestContext testContext) throws Exception {
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
    TestContextBootstrapper bootstrapper = resolveBootstrapper(testClass);
    MergedContextConfiguration mergedConfig = bootstrapper.buildMergedContextConfiguration();

    // The cache key is the hashCode of the MergedContextConfiguration
    int cacheKey = mergedConfig.hashCode();

    // Track the association between context configuration and test class
    contextCacheTracker.recordTestClassForContext(mergedConfig, className);

    // Detect and record the test annotation type for filtering
    String annotationType = TestAnnotationDetector.detectTestAnnotationType(testClass);
    contextCacheTracker.recordTestAnnotationType(mergedConfig, annotationType);

    logger.info("Test class {} uses context cache key {}", className, cacheKey);
  }

  @Override
  public void prepareTestInstance(@NonNull TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);

    if (className != null) {
      try {
        // Force context loading BEFORE capturing end time.
        // This listener runs with HIGHEST_PRECEDENCE (before
        // DependencyInjectionTestExecutionListener),
        // so the context has not been loaded yet when prepareTestInstance is called.
        // Calling getApplicationContext() triggers lazy context creation.
        org.springframework.context.ApplicationContext applicationContext =
            testContext.getApplicationContext();
        ContextCustomizerExtensionRegistry.registerAll(
            applicationContext.getBeansOfType(ContextCustomizerExtension.class).values());
        Instant contextLoadEndTime = Instant.now();

        Class<?> testClass = testContext.getTestClass();
        TestContextBootstrapper bootstrapper = resolveBootstrapper(testClass);
        MergedContextConfiguration mergedConfig = bootstrapper.buildMergedContextConfiguration();

        // Track when this context leaves the cache (@DirtiesContext / LRU eviction)
        registerRemovalListener(applicationContext, mergedConfig, testContext);

        // Calculate context loading time (listener-level measurement)
        Instant contextLoadStartTime = contextLoadStartTimes.get(testContext);
        long contextLoadDurationMs = 0;
        if (contextLoadStartTime != null) {
          contextLoadDurationMs =
              java.time.Duration.between(contextLoadStartTime, contextLoadEndTime).toMillis();
        }

        // Try to get enhanced profile data from ApplicationContextInitializer
        ContextProfileData profileData = null;
        if (applicationContext
            instanceof org.springframework.context.ConfigurableApplicationContext configurableCtx) {
          profileData =
              TimingTrackingApplicationContextInitializer.getContextProfileData(configurableCtx);
        }

        // Prefer initializer's precise timing (initialize() -> ContextRefreshedEvent)
        // over the listener's coarser measurement (beforeTestClass -> prepareTestInstance)
        if (profileData != null && profileData.getTotalLoadTimeMs() > 0) {
          contextLoadDurationMs = profileData.getTotalLoadTimeMs();
          logger.debug(
              "Using initializer timing for test class {} - Total time: {}ms, Memory: {}MB, Beans: {}",
              className,
              profileData.getTotalLoadTimeMs(),
              profileData.getMemoryUsedMB(),
              profileData.getBeanCreationMetrics() != null
                  ? profileData.getBeanCreationMetrics().getTotalBeansCreated()
                  : "unknown");
        }

        // Now check if this was a cache hit or miss
        // If the context was already tracked as created for another test, it's a hit -
        // unless it was removed from the cache in the meantime, then this is a re-creation
        Optional<ContextCacheEntry> entry = contextCacheTracker.getCacheEntry(mergedConfig);
        if (entry.isPresent() && entry.get().isCreated() && !entry.get().isCurrentlyRemoved()) {
          contextCacheTracker.recordContextCacheHit(mergedConfig);
          logger.debug(
              "Context cache hit for test class {} ({}ms)", className, contextLoadDurationMs);
        } else {
          // Try to get ContextDiagnostic information using getBeanProvider
          org.springframework.context.ConfigurableApplicationContext configurableContext =
              (org.springframework.context.ConfigurableApplicationContext) applicationContext;
          ContextDiagnostic contextDiagnostic =
              configurableContext.getBeanProvider(ContextDiagnostic.class).getIfAvailable();

          if (contextDiagnostic != null) {
            contextCacheTracker.recordContextCreation(
                mergedConfig,
                contextLoadDurationMs,
                contextDiagnostic.heapMemoryUsedBytes(),
                contextDiagnostic.availableProcessors());
          } else {
            contextCacheTracker.recordContextCreation(mergedConfig, contextLoadDurationMs);
          }

          // Capture bean definitions for context complexity analysis
          String[] beanNames = applicationContext.getBeanDefinitionNames();
          contextCacheTracker.recordBeanDefinitions(mergedConfig, beanNames);
          logger.debug(
              "New context created for test class {} with {} bean definitions ({}ms)",
              className,
              beanNames.length,
              contextLoadDurationMs);
        }
      } catch (Exception e) {
        logger.warn(
            "Failed to track context loading for test class {}: {}", className, e.getMessage());
      } finally {
        // Clean up context load timing
        contextLoadStartTimes.remove(testContext);
      }
    }
  }

  @Override
  public void afterTestClass(@NonNull TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);
    if (className != null) {
      executionTracker.recordTestClassEnd(className);
      logger.debug("Completed Spring Test Profiler for test class: {}", className);
    }

    // Clean up
    testClassNames.remove(testContext);
  }

  @Override
  public void beforeTestMethod(@NonNull TestContext testContext) throws Exception {
    String className = testClassNames.get(testContext);
    String methodName = testContext.getTestMethod().getName();

    if (className != null) {
      executionTracker.recordTestMethodStart(className, methodName);
      methodStartTimes.put(testContext, Instant.now());

      // Record which test method uses this context
      Optional<MergedContextConfiguration> config =
          contextCacheTracker.getContextForTestClass(className);
      if (config.isPresent()) {
        contextCacheTracker.recordTestMethodForContext(config.get(), className, methodName);
      }
    }
  }

  @Override
  public void afterTestExecution(@NonNull TestContext testContext) throws Exception {
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

  /**
   * Registers a ContextClosedEvent listener on the given context (once per context instance) to
   * record when it is removed from Spring's context cache. Both @DirtiesContext removal and LRU
   * eviction close the context synchronously, so the close event marks the removal time.
   */
  private static void registerRemovalListener(
      ApplicationContext applicationContext,
      MergedContextConfiguration mergedConfig,
      TestContext testContext) {
    if (!(applicationContext instanceof ConfigurableApplicationContext configurableContext)) {
      return;
    }
    if (!closeListenerRegistered.add(applicationContext)) {
      return;
    }

    // Capture the cache reference now; it is not reachable from within the close event
    ContextCache contextCache = SpringContextCacheAccessor.getContextCache(testContext);

    configurableContext.addApplicationListener(
        (ApplicationListener<ContextClosedEvent>)
            event -> {
              try {
                closeListenerRegistered.remove(event.getApplicationContext());
                if (reportGenerationStarted || isJvmShutdownInProgress()) {
                  return;
                }
                ContextRemovalReason reason =
                    ContextRemovalDetector.inferRemovalReason(contextCache);
                contextCacheTracker.recordContextRemoval(mergedConfig, Instant.now(), reason);
              } catch (Exception e) {
                logger.debug("Failed to record context removal", e);
              }
            });
  }

  /**
   * Contexts closed during JVM shutdown (e.g. Spring Boot's shutdown hook) were never removed from
   * the cache during the test run and must not be recorded as removals.
   */
  private static boolean isJvmShutdownInProgress() {
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      String frameClassName = element.getClassName();
      if (frameClassName.contains("SpringApplicationShutdownHook")
          || frameClassName.equals("java.lang.ApplicationShutdownHooks")) {
        return true;
      }
    }
    return false;
  }

  private TestStatus determineTestStatus(TestContext testContext) {
    if (testContext.getTestException() != null) {
      Throwable exception = testContext.getTestException();

      // Check for test abortion (AssumptionViolatedException or similar)
      if (exception.getClass().getSimpleName().contains("AssumptionViolated")
          || exception.getClass().getSimpleName().contains("TestAborted")) {
        return TestStatus.ABORTED;
      }

      return TestStatus.FAILED;
    }
    return TestStatus.PASSED;
  }

  /**
   * Resolves a TestContextBootstrapper for the given test class using reflection to access
   * BootstrapUtils. This is necessary because BootstrapUtils is package-private in Spring 5.x and
   * the public convenience method was only added in Spring 6.
   */
  private static TestContextBootstrapper resolveBootstrapper(Class<?> testClass) {
    try {
      Class<?> bootstrapUtilsClass =
          Class.forName("org.springframework.test.context.BootstrapUtils");

      // Spring 6+: public static resolveTestContextBootstrapper(Class<?>)
      try {
        Method resolveMethod =
            bootstrapUtilsClass.getMethod("resolveTestContextBootstrapper", Class.class);
        return (TestContextBootstrapper) resolveMethod.invoke(null, testClass);
      } catch (NoSuchMethodException e) {
        // Fall through to Spring 5 path
      }

      // Spring 5: package-private createBootstrapContext + resolveTestContextBootstrapper
      Method createCtx =
          bootstrapUtilsClass.getDeclaredMethod("createBootstrapContext", Class.class);
      createCtx.setAccessible(true);
      Object bootstrapContext = createCtx.invoke(null, testClass);

      Class<?> bootstrapContextClass =
          Class.forName("org.springframework.test.context.BootstrapContext");
      Method resolveMethod =
          bootstrapUtilsClass.getDeclaredMethod(
              "resolveTestContextBootstrapper", bootstrapContextClass);
      resolveMethod.setAccessible(true);
      return (TestContextBootstrapper) resolveMethod.invoke(null, bootstrapContext);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to resolve TestContextBootstrapper for " + testClass.getName(), e);
    }
  }

  /**
   * Registers a shutdown hook to ensure report generation when JVM exits. This is called once when
   * the first test class is processed.
   */
  private static void registerShutdownHook() {
    if (!shutdownHookRegistered) {
      synchronized (SpringTestProfilerListener.class) {
        if (!shutdownHookRegistered) {
          Runtime.getRuntime()
              .addShutdownHook(
                  new Thread(
                      () -> {
                        try {
                          generateReport();
                        } catch (Exception e) {
                          System.err.println(
                              "Spring Test Profiler: Failed to generate report: " + e.getMessage());
                          e.printStackTrace(System.err);
                        }
                      },
                      "SpringTestProfilerReportGenerator"));
          shutdownHookRegistered = true;
          logger.debug("Registered shutdown hook for Spring Test Profiler report generation");
        }
      }
    }
  }

  /** Called by the shutdown hook or manually to generate the final report. */
  public static void generateReport() {
    synchronized (SpringTestProfilerListener.class) {
      if (!reportGenerated) {
        reportGenerationStarted = true;
        logger.info("Generating Spring Test Profiler");
        executionTracker.stopTracking();

        // Get context cache statistics including our custom tracking
        SpringContextCacheAccessor.CacheStatistics springStats = getCacheStatistics();

        // Generate report with both execution and context cache data
        reporter.generateReport(executionTracker, springStats, contextCacheTracker);

        // Clear data
        contextCacheTracker.clear();
        ContextCustomizerExtensionRegistry.clear();
        reportGenerated = true;
      }
    }
  }

  /** Exposes the shared tracker for integration tests. */
  static ContextCacheTracker getContextCacheTracker() {
    return contextCacheTracker;
  }

  /** Gets the Spring ContextCache if available. */
  public static ContextCache getContextCache() {
    TestContext context = lastTestContext.get();
    if (context != null) {
      return SpringContextCacheAccessor.getContextCache(context);
    }
    return null;
  }

  /** Gets cache statistics from Spring's DefaultContextCache. */
  public static SpringContextCacheAccessor.CacheStatistics getCacheStatistics() {
    ContextCache cache = getContextCache();
    return SpringContextCacheAccessor.getCacheStatistics(cache);
  }
}
