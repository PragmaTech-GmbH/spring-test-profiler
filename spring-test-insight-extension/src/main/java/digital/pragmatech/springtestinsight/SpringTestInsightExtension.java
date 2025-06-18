package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.extension.*;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.engine.TestExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpringTestInsightExtension implements TestWatcher, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ExtensionContext.Store.CloseableResource {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringTestInsightExtension.class);
    private static final String STORE_KEY = "spring-test-insight";
    
    private final Map<String, TestExecutionData> testExecutions = new ConcurrentHashMap<>();
    private final TestExecutionReporter reporter = new TestExecutionReporter();
    private static volatile boolean reportGenerated = false;
    private static String currentPhase = null;
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        logger.debug("Starting Spring Test Insight for test class: {}", testClass.getName());
        
        // Detect which Maven phase we're in based on test naming conventions
        String detectedPhase = detectMavenPhase(testClass);
        
        // Reset report generation flag if we've moved to a different phase
        synchronized (SpringTestInsightExtension.class) {
            if (currentPhase == null) {
                currentPhase = detectedPhase;
            } else if (!currentPhase.equals(detectedPhase)) {
                logger.info("Phase change detected: {} -> {}. Resetting report generation.", currentPhase, detectedPhase);
                reportGenerated = false;
                currentPhase = detectedPhase;
            }
        }
        
        // Register this extension as a closeable resource in the root store
        // This ensures close() is called after all tests complete
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
            .getOrComputeIfAbsent("spring-test-insight-closeable", k -> this, ExtensionContext.Store.CloseableResource.class);
        
        getStore(context).put(STORE_KEY, this);
        SpringContextCacheStatistics.startTracking();
        
        // Analyze the test class to determine its context configuration
        ContextConfigurationDetector.analyzeTestClass(testClass);
    }
    
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.debug("Completing Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
        
        TestClassExecutionData classData = new TestClassExecutionData(
            context.getRequiredTestClass().getName(),
            testExecutions,
            SpringContextCacheStatistics.getStatistics()
        );
        
        reporter.addTestClassData(classData);
        
        // Report generation is now handled in the close() method
    }
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String testId = getTestId(context);
        testExecutions.put(testId, new TestExecutionData(testId, Instant.now()));
    }
    
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        String testId = getTestId(context);
        TestExecutionData data = testExecutions.get(testId);
        if (data != null) {
            data.setEndTime(Instant.now());
        }
    }
    
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        String testId = getTestId(context);
        TestExecutionData data = new TestExecutionData(testId, Instant.now());
        data.setStatus(TestStatus.DISABLED);
        reason.ifPresent(data::setReason);
        testExecutions.put(testId, data);
    }
    
    @Override
    public void testSuccessful(ExtensionContext context) {
        updateTestStatus(context, TestStatus.PASSED);
    }
    
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        updateTestStatus(context, TestStatus.ABORTED, cause);
    }
    
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        updateTestStatus(context, TestStatus.FAILED, cause);
    }
    
    private void updateTestStatus(ExtensionContext context, TestStatus status) {
        updateTestStatus(context, status, null);
    }
    
    private void updateTestStatus(ExtensionContext context, TestStatus status, Throwable cause) {
        String testId = getTestId(context);
        TestExecutionData data = testExecutions.get(testId);
        if (data != null) {
            data.setStatus(status);
            if (cause != null) {
                data.setThrowable(cause);
            }
        }
    }
    
    private String getTestId(ExtensionContext context) {
        return context.getRequiredTestClass().getName() + "#" + context.getRequiredTestMethod().getName();
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
    }
    
    @Override
    public void close() throws Throwable {
        // This method is called after all tests have completed
        // Use synchronization to ensure the report is only generated once per phase
        synchronized (SpringTestInsightExtension.class) {
            if (!reportGenerated) {
                String phase = currentPhase != null ? currentPhase : "unknown";
                logger.info("All tests completed for {} phase. Generating Spring Test Insight report...", phase);
                SpringContextCacheStatistics.stopTracking();
                reporter.generateReport(phase);
                ContextConfigurationDetector.clear();
                reportGenerated = true;
            } else {
                logger.debug("Report already generated for current phase, skipping...");
            }
        }
    }
    
    /**
     * Detects which Maven phase we're in based on test class naming conventions.
     * Integration tests typically end with 'IT' and are run by failsafe plugin.
     * Unit tests are run by surefire plugin.
     */
    private String detectMavenPhase(Class<?> testClass) {
        String className = testClass.getSimpleName();
        
        // Maven failsafe plugin runs integration tests (typically named *IT)
        if (className.endsWith("IT") || className.contains("Integration")) {
            return "failsafe";
        }
        
        // Maven surefire plugin runs unit tests
        return "surefire";
    }
}