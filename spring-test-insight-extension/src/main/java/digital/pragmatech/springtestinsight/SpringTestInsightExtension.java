package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.extension.*;
import org.junit.platform.engine.TestExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpringTestInsightExtension implements TestWatcher, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringTestInsightExtension.class);
    private static final String STORE_KEY = "spring-test-insight";
    
    private final Map<String, TestExecutionData> testExecutions = new ConcurrentHashMap<>();
    private final SpringContextCacheStatistics cacheStatistics = new SpringContextCacheStatistics();
    private final TestExecutionReporter reporter = new TestExecutionReporter();
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        logger.debug("Starting Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
        getStore(context).put(STORE_KEY, this);
        cacheStatistics.startTracking();
    }
    
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.debug("Completing Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
        cacheStatistics.stopTracking();
        
        TestClassExecutionData classData = new TestClassExecutionData(
            context.getRequiredTestClass().getName(),
            testExecutions,
            cacheStatistics.getStatistics()
        );
        
        reporter.addTestClassData(classData);
        
        if (isLastTestClass(context)) {
            reporter.generateReport();
        }
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
    
    private boolean isLastTestClass(ExtensionContext context) {
        // This is a simplified check - in production, we'd need more sophisticated logic
        return context.getRoot().equals(context.getParent().orElse(null));
    }
}