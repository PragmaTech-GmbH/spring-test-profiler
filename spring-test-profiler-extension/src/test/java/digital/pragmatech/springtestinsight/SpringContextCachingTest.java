package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ContextConfiguration(classes = SpringContextCachingTest.FirstConfig.class)
class SpringContextCachingTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testFirstContext() {
        assertNotNull(applicationContext);
    }
    
    @Configuration
    static class FirstConfig {
    }
    
    @ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
    @ContextConfiguration(classes = SecondConfig.class)
    static class DifferentContextTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        void testSecondContext() {
            // This should create a new context (cache miss)
            assertNotNull(applicationContext);
        }
    }
    
    @Configuration
    static class SecondConfig {
    }
    
    @ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
    @ContextConfiguration(classes = FirstConfig.class)
    static class SameContextTest {
        
        @Autowired
        private ApplicationContext applicationContext;
        
        @Test
        void testReuseFirstContext() {
            // This should reuse the first context (cache hit)
            assertNotNull(applicationContext);
        }
    }
}