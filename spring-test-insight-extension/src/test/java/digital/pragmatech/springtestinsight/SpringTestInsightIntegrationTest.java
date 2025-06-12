package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ContextConfiguration(classes = SpringTestInsightIntegrationTest.TestConfig.class)
class SpringTestInsightIntegrationTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private TestService testService;
    
    @Test
    void testSpringContextLoads() {
        assertNotNull(applicationContext);
        assertNotNull(testService);
        assertEquals("Hello from TestService", testService.getMessage());
    }
    
    @Test
    void testServiceBehavior() {
        String result = testService.processMessage("Test");
        assertEquals("Processed: Test", result);
    }
    
    @Test
    void testMultipleTestsShareContext() {
        // This test should reuse the same Spring context as the previous tests
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("testService"));
    }
    
    @Configuration
    static class TestConfig {
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }
    
    static class TestService {
        
        public String getMessage() {
            return "Hello from TestService";
        }
        
        public String processMessage(String input) {
            return "Processed: " + input;
        }
    }
}