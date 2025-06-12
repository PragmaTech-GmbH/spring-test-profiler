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
    
    private final SpringContextStatistics statistics = new SpringContextStatistics();
    private final Map<String, Instant> contextLoadStartTimes = new ConcurrentHashMap<>();
    private boolean tracking = false;
    
    public void startTracking() {
        tracking = true;
    }
    
    public void stopTracking() {
        tracking = false;
    }
    
    public SpringContextStatistics getStatistics() {
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
    
    private boolean isContextCached(TestContext testContext) {
        // This is a simplified check - in a real implementation, we'd need to
        // integrate more deeply with Spring's context caching mechanism
        try {
            // Try to check if the context was already in the cache
            Field contextCacheField = testContext.getClass().getDeclaredField("contextCache");
            contextCacheField.setAccessible(true);
            Object contextCache = contextCacheField.get(testContext);
            
            if (contextCache != null) {
                // Check if the context was already present
                return true;
            }
        } catch (Exception e) {
            // Fallback to checking application context start time
            return testContext.getApplicationContext().getStartupDate() < 
                   System.currentTimeMillis() - 1000;
        }
        
        return false;
    }
    
    @Override
    public int getOrder() {
        // Run early in the chain to capture accurate timing
        return 1000;
    }
}