package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpringTestInsightExtension implements TestWatcher, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ExtensionContext.Store.CloseableResource {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringTestInsightExtension.class);
    private static final String STORE_KEY = "spring-test-insight";
    
    private static final TestExecutionTracker executionTracker = new TestExecutionTracker();
    private final TestExecutionReporter reporter = new TestExecutionReporter();
    private static volatile boolean reportGenerated = false;
    private static String currentPhase = null;
    private String currentTestClass;
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        logger.debug("Starting Spring Test Insight for test class: {}", testClass.getName());
        
        // Detect build tool and phase based on system properties and test naming conventions
        String detectedPhase = detectBuildToolPhase(testClass);
        
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
        
        // Start tracking if this is the first test class
        if (executionTracker.getTotalTestClasses() == 0) {
            executionTracker.startTracking();
        }
        
        currentTestClass = testClass.getName();
        executionTracker.recordTestClassStart(currentTestClass);
        
        // Analyze the test class to determine its context configuration
        ContextConfigurationDetector.analyzeTestClass(testClass);
    }
    
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.debug("Completing Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
        
        String className = context.getRequiredTestClass().getName();
        executionTracker.recordTestClassEnd(className);
        
        // Report generation is now handled in the close() method
    }
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String methodName = context.getRequiredTestMethod().getName();
        executionTracker.recordTestMethodStart(currentTestClass, methodName);
    }
    
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Status will be updated by the TestWatcher methods
    }
    
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        String methodName = context.getRequiredTestMethod().getName();
        executionTracker.recordTestMethodEnd(currentTestClass, methodName, TestStatus.DISABLED);
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
        String methodName = context.getRequiredTestMethod().getName();
        executionTracker.recordTestMethodEnd(currentTestClass, methodName, status);
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
                executionTracker.stopTracking();
                reporter.generateReport(phase, executionTracker, SpringContextCacheStatistics.getCacheStatistics());
                ContextConfigurationDetector.clear();
                reportGenerated = true;
            } else {
                logger.debug("Report already generated for current phase, skipping...");
            }
        }
    }
    
    /**
     * Detects the build tool and test phase based on system properties and test class naming conventions.
     * For Maven: distinguishes between surefire (unit tests) and failsafe (integration tests).
     * For Gradle: distinguishes between test and integrationTest tasks.
     */
    private String detectBuildToolPhase(Class<?> testClass) {
        // First, detect the build tool
        String buildTool = detectBuildTool();
        
        // Then, detect the test phase based on naming conventions
        String className = testClass.getSimpleName();
        boolean isIntegrationTest = className.endsWith("IT") || 
                                   className.endsWith("IntegrationTest") || 
                                   className.contains("Integration");
        
        // Return phase based on build tool
        if ("maven".equals(buildTool)) {
            return isIntegrationTest ? "failsafe" : "surefire";
        } else if ("gradle".equals(buildTool)) {
            return isIntegrationTest ? "integrationTest" : "test";
        } else {
            // Unknown build tool, use generic phase names
            return isIntegrationTest ? "integration" : "unit";
        }
    }
    
    /**
     * Detects the build tool being used based on system properties and classpath indicators.
     */
    private String detectBuildTool() {
        // Check for Maven-specific system properties
        if (System.getProperty("maven.home") != null || 
            System.getProperty("maven.version") != null ||
            System.getProperty("surefire.test.class.path") != null ||
            System.getProperty("basedir") != null && System.getProperty("basedir").contains("target")) {
            return "maven";
        }
        
        // Check for Gradle-specific system properties
        if (System.getProperty("gradle.home") != null ||
            System.getProperty("gradle.version") != null ||
            System.getProperty("org.gradle.test.worker") != null ||
            System.getProperty("gradle.user.home") != null) {
            return "gradle";
        }
        
        // Check classpath for build tool indicators
        String classpath = System.getProperty("java.class.path", "");
        if (classpath.contains("/target/") || classpath.contains("\\target\\") || 
            classpath.contains("maven")) {
            return "maven";
        } else if (classpath.contains("/build/") || classpath.contains("\\build\\") || 
                   classpath.contains("gradle")) {
            return "gradle";
        }
        
        // Default to unknown
        return "unknown";
    }
}