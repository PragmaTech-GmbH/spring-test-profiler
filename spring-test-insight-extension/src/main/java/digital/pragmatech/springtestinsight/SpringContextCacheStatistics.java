package digital.pragmatech.springtestinsight;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringContextCacheStatistics extends AbstractTestExecutionListener {
    
    // Use static fields to share data across all instances
    private static final SpringContextStatistics statistics = new SpringContextStatistics();
    private static final Map<String, Instant> contextLoadStartTimes = new ConcurrentHashMap<>();
    private static boolean tracking = true; // Start tracking by default since Spring listeners run before JUnit extensions
    
    public SpringContextCacheStatistics() {
        // Constructor - instance created by Spring
    }
    
    public static void startTracking() {
        tracking = true;
    }
    
    public static void stopTracking() {
        // Don't actually stop tracking - we want to track across all test classes
        // This method is called at the end to generate the final report
    }
    
    public static SpringContextStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        if (!tracking) return;
        
        String contextKey = generateContextKey(testContext);
        contextLoadStartTimes.put(contextKey, Instant.now());
    }
    
    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        if (!tracking) return;
        
        String contextKey = generateContextKey(testContext);
        Instant startTime = contextLoadStartTimes.get(contextKey);
        
        if (startTime != null) {
            Duration loadTime = Duration.between(startTime, Instant.now());
            
            // Check if this was a cache hit or miss by inspecting the context cache
            if (isContextCached(testContext)) {
                statistics.recordCacheHit(contextKey);
            } else {
                statistics.recordContextLoad(contextKey, loadTime);
            }
            
            contextLoadStartTimes.remove(contextKey);
        }
    }
    
    private String generateContextKey(TestContext testContext) {
        return testContext.getTestClass().getName() + "@" + 
               testContext.getApplicationContext().getId();
    }
    
    private static final Map<String, String> seenContexts = new ConcurrentHashMap<>();
    
    private boolean isContextCached(TestContext testContext) {
        String contextId = testContext.getApplicationContext().getId();
        String contextKey = generateContextKey(testContext);
        
        // If we've seen this context ID before, it's likely a cache hit
        if (seenContexts.containsKey(contextId)) {
            return true;
        } else {
            // First time seeing this context, mark it as seen
            seenContexts.put(contextId, contextKey);
            return false;
        }
    }
    
    @Override
    public int getOrder() {
        // Run early in the chain to capture accurate timing
        return 1000;
    }
}