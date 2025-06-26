package digital.pragmatech.springtestinsight;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.cache.ContextCache;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple test execution listener that captures a TestContext reference
 * so we can access Spring's DefaultContextCache later.
 */
public class SpringContextCacheStatistics extends AbstractTestExecutionListener {
    
    // Hold a reference to a TestContext so we can access the cache later
    private static final AtomicReference<TestContext> lastTestContext = new AtomicReference<>();
    
    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        // Just capture the TestContext reference
        lastTestContext.set(testContext);
    }
    
    /**
     * Gets the Spring ContextCache if available.
     */
    public static ContextCache getContextCache() {
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
        ContextCache cache = getContextCache();
        return SpringContextCacheAccessor.getCacheStatistics(cache);
    }
    
    @Override
    public int getOrder() {
        // Run early in the chain
        return 1000;
    }
}