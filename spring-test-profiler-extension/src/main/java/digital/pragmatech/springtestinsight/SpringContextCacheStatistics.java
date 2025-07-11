package digital.pragmatech.springtestinsight;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.cache.ContextCache;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple test execution listener that captures a TestContext reference
 * so we can access Spring's DefaultContextCache later.
 * 
 * @deprecated This functionality has been consolidated into {@link SpringTestInsightListener}.
 *             This class will be removed in a future version.
 */
@Deprecated
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
     * @deprecated Use {@link SpringTestInsightListener#getContextCache()} instead.
     */
    @Deprecated
    public static ContextCache getContextCache() {
        return SpringTestInsightListener.getContextCache();
    }
    
    /**
     * Gets cache statistics from Spring's DefaultContextCache.
     * @deprecated Use {@link SpringTestInsightListener#getCacheStatistics()} instead.
     */
    @Deprecated
    public static SpringContextCacheAccessor.CacheStatistics getCacheStatistics() {
        return SpringTestInsightListener.getCacheStatistics();
    }
    
    @Override
    public int getOrder() {
        // Run early in the chain
        return 1000;
    }
}