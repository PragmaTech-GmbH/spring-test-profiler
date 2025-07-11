package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class SpringTestInsightExtension implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

    private static final Logger logger = LoggerFactory.getLogger(SpringTestInsightExtension.class);
    private static final String STORE_KEY = "spring-test-profiler";

    private static volatile boolean reportGenerated = false;
    private static String currentPhase = null;

    // Enum to represent the detected execution environment
    public enum ExecutionEnvironment {
        MAVEN_SUREFIRE,
        MAVEN_FAILSAFE,
        GRADLE_TEST,
        GRADLE_INTEGRATION_TEST,
        INTELLIJ,
        ECLIPSE,
        VSCODE,
        NETBEANS,
        UNKNOWN
    }

    private static ExecutionEnvironment detectedEnvironment;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        logger.debug("Starting Spring Test Insight for test class: {}", testClass.getName());

        // Detect execution environment using enhanced detection approach
        if (detectedEnvironment == null) {
            detectedEnvironment = detectExecutionEnvironment(testClass);
            logger.info("Detected execution environment: {}", detectedEnvironment);
        }

        String detectedPhase = getPhaseFromEnvironment(detectedEnvironment);

        // Reset report generation flag if we've moved to a different phase
        synchronized (SpringTestInsightExtension.class) {
            if (currentPhase == null) {
                currentPhase = detectedPhase;
            } else if (!currentPhase.equals(detectedPhase)) {
                logger.info("Phase change detected: {} -> {}. Resetting report generation.", currentPhase, detectedPhase);
                reportGenerated = false;
                SpringTestInsightListener.resetReportGeneration();
                currentPhase = detectedPhase;
            }
        }

        // Register this extension as a closeable resource in the root store
        // This ensures close() is called after all tests complete
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
            .getOrComputeIfAbsent("spring-test-profiler-closeable", k -> this, ExtensionContext.Store.CloseableResource.class);

        getStore(context).put(STORE_KEY, this);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.debug("Completing Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
        // Report generation is now handled in the close() method
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

                // Call the SpringTestInsightListener to generate the report
                SpringTestInsightListener.generateReport(phase);
                reportGenerated = true;
            } else {
                logger.debug("Report already generated for current phase, skipping...");
            }
        }
    }

    /**
     * Detects the execution environment using stack trace analysis (inspired by ToolDetectionExtension)
     * combined with system property and test naming convention detection.
     */
    private ExecutionEnvironment detectExecutionEnvironment(Class<?> testClass) {
        // First try stack trace analysis for IDE detection
        ExecutionEnvironment stackTraceResult = detectFromStackTrace();
        if (stackTraceResult != ExecutionEnvironment.UNKNOWN) {
            return stackTraceResult;
        }

        // Fall back to build tool detection with test phase inference
        return detectBuildToolPhase(testClass);
    }

    /**
     * Detects execution environment from stack trace analysis.
     */
    private ExecutionEnvironment detectFromStackTrace() {
        try {
            throw new RuntimeException("Environment Detection");
        } catch (RuntimeException e) {
            String stackTrace = Arrays.toString(e.getStackTrace());

            if (stackTrace.contains("com.intellij.rt.junit")) {
                return ExecutionEnvironment.INTELLIJ;
            }
            if (stackTrace.contains("org.eclipse.jdt.internal.junit")) {
                return ExecutionEnvironment.ECLIPSE;
            }
            if (stackTrace.contains("com.microsoft.java.test.runner")) {
                return ExecutionEnvironment.VSCODE;
            }
            if (stackTrace.contains("org.netbeans.modules")) {
                return ExecutionEnvironment.NETBEANS;
            }
            if (stackTrace.contains("org.apache.maven.surefire")) {
                // Maven detected, but need to determine surefire vs failsafe
                return ExecutionEnvironment.UNKNOWN; // Will be refined by detectBuildToolPhase
            }
            if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
                // Gradle detected, but need to determine test vs integrationTest
                return ExecutionEnvironment.UNKNOWN; // Will be refined by detectBuildToolPhase
            }

            return ExecutionEnvironment.UNKNOWN;
        }
    }

    /**
     * Detects the build tool and test phase based on system properties and test class naming conventions.
     * For Maven: distinguishes between surefire (unit tests) and failsafe (integration tests).
     * For Gradle: distinguishes between test and integrationTest tasks.
     */
    private ExecutionEnvironment detectBuildToolPhase(Class<?> testClass) {
        // First, detect the build tool
        String buildTool = detectBuildTool();

        // Then, detect the test phase based on naming conventions
        String className = testClass.getSimpleName();
        boolean isIntegrationTest = className.endsWith("IT") ||
                                   className.endsWith("IntegrationTest") ||
                                   className.contains("Integration");

        // Return phase based on build tool
        if ("maven".equals(buildTool)) {
            return isIntegrationTest ? ExecutionEnvironment.MAVEN_FAILSAFE : ExecutionEnvironment.MAVEN_SUREFIRE;
        } else if ("gradle".equals(buildTool)) {
            return isIntegrationTest ? ExecutionEnvironment.GRADLE_INTEGRATION_TEST : ExecutionEnvironment.GRADLE_TEST;
        } else {
            return ExecutionEnvironment.UNKNOWN;
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

    /**
     * Converts execution environment to phase string for reporting.
     */
    private String getPhaseFromEnvironment(ExecutionEnvironment environment) {
        switch (environment) {
            case MAVEN_SUREFIRE:
                return "surefire";
            case MAVEN_FAILSAFE:
                return "failsafe";
            case GRADLE_TEST:
                return "gradle-test";
            case GRADLE_INTEGRATION_TEST:
                return "gradle-integrationTest";
            case INTELLIJ:
                return "intellij";
            case ECLIPSE:
                return "eclipse";
            case VSCODE:
                return "vscode";
            case NETBEANS:
                return "netbeans";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }

    /**
     * Gets the detected execution environment.
     */
    public static ExecutionEnvironment getDetectedEnvironment() {
        return detectedEnvironment;
    }
}
