package digital.pragmatech.springtestinsight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TestExecutionReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionReporter.class);
    private static final String REPORT_DIR = "target/spring-test-insight";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final List<TestClassExecutionData> testClassData = new CopyOnWriteArrayList<>();
    
    public synchronized void addTestClassData(TestClassExecutionData data) {
        testClassData.add(data);
    }
    
    public void generateReport() {
        try {
            Path reportDir = Paths.get(REPORT_DIR);
            Files.createDirectories(reportDir);
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            Path reportFile = reportDir.resolve("test-insight-report-" + timestamp + ".html");
            
            String htmlContent = generateHtml();
            Files.write(reportFile, htmlContent.getBytes());
            
            logger.info("Spring Test Insight report generated: {}", reportFile.toAbsolutePath());
            
            // Also create a latest.html symlink for easy access
            Path latestLink = reportDir.resolve("latest.html");
            Files.deleteIfExists(latestLink);
            Files.write(latestLink, htmlContent.getBytes());
            
        } catch (IOException e) {
            logger.error("Failed to generate Spring Test Insight report", e);
        }
    }
    
    private String generateHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Spring Test Insight Report</title>\n");
        html.append("    <link rel=\"icon\" href=\"data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🧪</text></svg>\">\n");
        html.append(generateStyles());
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <h1>Spring Test Insight Report</h1>\n");
        html.append("        <div class=\"timestamp\">Generated at: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</div>\n");
        
        // Theoretical background section
        html.append(generateTheorySection());
        
        // Summary section
        html.append(generateSummarySection());
        
        // Context caching statistics
        html.append(generateContextCachingSection());
        
        // Cache keys section
        html.append(generateCacheKeysSection());
        
        // Test execution details
        html.append(generateTestExecutionSection());
        
        html.append("    </div>\n");
        html.append(generateScript());
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    private String generateStyles() {
        return """
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    background-color: #f5f5f5;
                }
                
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    padding: 20px;
                }
                
                h1 {
                    color: #2c3e50;
                    margin-bottom: 10px;
                }
                
                h2 {
                    color: #34495e;
                    margin-top: 30px;
                    margin-bottom: 15px;
                }
                
                .timestamp {
                    color: #7f8c8d;
                    font-size: 14px;
                    margin-bottom: 20px;
                }
                
                .theory-section {
                    background: white;
                    border-radius: 8px;
                    padding: 30px;
                    margin-bottom: 30px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                
                .theory-content h3 {
                    color: #2c3e50;
                    margin-top: 25px;
                    margin-bottom: 15px;
                }
                
                .theory-content ul {
                    margin: 15px 0;
                    padding-left: 25px;
                }
                
                .theory-content li {
                    margin-bottom: 8px;
                    line-height: 1.6;
                }
                
                .cache-behavior {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 20px;
                    margin: 25px 0;
                }
                
                .cache-hit, .cache-miss {
                    background: #f8f9fa;
                    border-radius: 8px;
                    padding: 20px;
                    border-left: 4px solid;
                }
                
                .cache-hit {
                    border-color: #27ae60;
                }
                
                .cache-miss {
                    border-color: #e74c3c;
                }
                
                .cache-hit h4, .cache-miss h4 {
                    margin-top: 0;
                    margin-bottom: 10px;
                    font-size: 16px;
                }
                
                .docs-links {
                    background: #ecf0f1;
                    border-radius: 8px;
                    padding: 20px;
                    margin-top: 25px;
                }
                
                .docs-links h3 {
                    margin-top: 0;
                    color: #2c3e50;
                }
                
                .docs-links a {
                    color: #3498db;
                    text-decoration: none;
                }
                
                .docs-links a:hover {
                    text-decoration: underline;
                }
                
                .summary-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    margin-bottom: 30px;
                }
                
                .summary-card {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    text-align: center;
                }
                
                .summary-card h3 {
                    color: #7f8c8d;
                    font-size: 14px;
                    font-weight: normal;
                    margin-bottom: 10px;
                }
                
                .summary-card .value {
                    font-size: 36px;
                    font-weight: bold;
                }
                
                .summary-card.passed .value { color: #27ae60; }
                .summary-card.failed .value { color: #e74c3c; }
                .summary-card.disabled .value { color: #95a5a6; }
                .summary-card.aborted .value { color: #f39c12; }
                
                .cache-stats {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    margin-bottom: 30px;
                }
                
                .cache-metric {
                    display: flex;
                    justify-content: space-between;
                    padding: 10px 0;
                    border-bottom: 1px solid #ecf0f1;
                }
                
                .cache-metric:last-child {
                    border-bottom: none;
                }
                
                .cache-metric .label {
                    font-weight: 500;
                }
                
                .cache-metric .value {
                    font-weight: bold;
                }
                
                .hit-rate {
                    color: #27ae60;
                }
                
                .test-class {
                    background: white;
                    border-radius: 8px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                
                .test-class-header {
                    padding: 15px 20px;
                    background: #34495e;
                    color: white;
                    border-radius: 8px 8px 0 0;
                    cursor: pointer;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                
                .test-class-header:hover {
                    background: #2c3e50;
                }
                
                .test-class-stats {
                    display: flex;
                    gap: 15px;
                    font-size: 14px;
                }
                
                .test-methods {
                    padding: 20px;
                    display: none;
                }
                
                .test-methods.show {
                    display: block;
                }
                
                .test-method {
                    padding: 10px;
                    margin-bottom: 10px;
                    border-left: 4px solid;
                    background: #f8f9fa;
                }
                
                .test-method.passed {
                    border-color: #27ae60;
                }
                
                .test-method.failed {
                    border-color: #e74c3c;
                }
                
                .test-method.disabled {
                    border-color: #95a5a6;
                }
                
                .test-method.aborted {
                    border-color: #f39c12;
                }
                
                .test-method-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                
                .test-method-name {
                    font-family: 'Courier New', monospace;
                    font-weight: 500;
                }
                
                .test-duration {
                    color: #7f8c8d;
                    font-size: 14px;
                }
                
                .execution-overview {
                    background: #e8f5e8;
                    border-radius: 8px;
                    padding: 15px;
                    margin-bottom: 20px;
                    border-left: 4px solid #27ae60;
                }
                
                .test-class-package {
                    font-size: 12px;
                    color: #7f8c8d;
                    font-family: 'Courier New', monospace;
                }
                
                .context-info {
                    background: #f0f8ff;
                    border-radius: 6px;
                    padding: 15px;
                    margin-bottom: 15px;
                    border-left: 4px solid #3498db;
                }
                
                .context-info h4 {
                    margin: 0 0 10px 0;
                    color: #2c3e50;
                    font-size: 14px;
                }
                
                .context-metrics {
                    display: flex;
                    gap: 15px;
                    flex-wrap: wrap;
                }
                
                .context-metric {
                    background: white;
                    padding: 5px 10px;
                    border-radius: 4px;
                    font-size: 13px;
                    border: 1px solid #bdc3c7;
                }
                
                .test-method-info {
                    display: flex;
                    gap: 10px;
                    align-items: center;
                }
                
                .test-status {
                    font-size: 12px;
                    padding: 2px 6px;
                    border-radius: 3px;
                    background: #ecf0f1;
                    color: #2c3e50;
                    text-transform: uppercase;
                    font-weight: bold;
                }
                
                .test-reason {
                    margin-top: 8px;
                    padding: 8px;
                    background: #fff3cd;
                    border-radius: 4px;
                    font-size: 13px;
                    border-left: 3px solid #ffc107;
                }
                
                .error-details {
                    margin-top: 10px;
                    padding: 10px;
                    background: #fee;
                    border-radius: 4px;
                    font-size: 12px;
                    border-left: 3px solid #e74c3c;
                }
                
                .error-details h5 {
                    margin: 0 0 8px 0;
                    color: #c0392b;
                    font-size: 13px;
                }
                
                .error-details pre {
                    margin: 0;
                    font-family: 'Courier New', monospace;
                    font-size: 11px;
                    white-space: pre-wrap;
                    overflow-x: auto;
                    line-height: 1.4;
                }
                
                .cache-keys-section {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    margin-bottom: 30px;
                }
                
                .cache-key-item {
                    background: #f8f9fa;
                    border-radius: 6px;
                    padding: 15px;
                    margin-bottom: 15px;
                    border-left: 4px solid #3498db;
                }
                
                .cache-key-name {
                    font-family: 'Courier New', monospace;
                    font-weight: bold;
                    color: #2c3e50;
                    margin-bottom: 8px;
                }
                
                .cache-key-usage {
                    display: flex;
                    gap: 15px;
                    margin-top: 10px;
                }
                
                .cache-key-stat {
                    background: white;
                    padding: 5px 10px;
                    border-radius: 4px;
                    font-size: 13px;
                    border: 1px solid #bdc3c7;
                }
                
                .test-classes-list {
                    margin-top: 10px;
                }
                
                .test-class-tag {
                    display: inline-block;
                    background: #e3f2fd;
                    color: #1976d2;
                    padding: 3px 8px;
                    border-radius: 12px;
                    font-size: 12px;
                    margin: 2px 4px 2px 0;
                    border: 1px solid #bbdefb;
                }
            </style>
            """;
    }
    
    private String generateTheorySection() {
        StringBuilder html = new StringBuilder();
        html.append("        <div class=\"theory-section\">\n");
        html.append("            <h2>Understanding Spring Test Context Caching</h2>\n");
        html.append("            <div class=\"theory-content\">\n");
        html.append("                <p>Spring Test Framework provides <strong>context caching</strong> to improve test performance by reusing ApplicationContext instances across multiple test classes when they share the same configuration.</p>\n");
        html.append("                \n");
        html.append("                <h3>How Context Caching Works</h3>\n");
        html.append("                <p>Spring generates a <strong>cache key</strong> based on:</p>\n");
        html.append("                <ul>\n");
        html.append("                    <li><strong>Context Loader</strong> - The class responsible for loading the context</li>\n");
        html.append("                    <li><strong>Locations & Classes</strong> - Configuration files/classes specified in @ContextConfiguration</li>\n");
        html.append("                    <li><strong>Active Profiles</strong> - Profiles activated via @ActiveProfiles</li>\n");
        html.append("                    <li><strong>Property Sources</strong> - Test property sources from @TestPropertySource</li>\n");
        html.append("                    <li><strong>Context Initializers</strong> - Custom context initializers</li>\n");
        html.append("                    <li><strong>Context Customizers</strong> - Customizers from test slices like @WebMvcTest</li>\n");
        html.append("                </ul>\n");
        html.append("                \n");
        html.append("                <div class=\"cache-behavior\">\n");
        html.append("                    <div class=\"cache-hit\">\n");
        html.append("                        <h4>🎯 Cache Hit</h4>\n");
        html.append("                        <p>When a test class has the <em>same exact configuration</em> as a previous test, Spring reuses the existing ApplicationContext. This results in faster test execution.</p>\n");
        html.append("                    </div>\n");
        html.append("                    <div class=\"cache-miss\">\n");
        html.append("                        <h4>🔄 Cache Miss</h4>\n");
        html.append("                        <p>When a test class has a <em>different configuration</em>, Spring creates a new ApplicationContext. This takes more time but ensures test isolation.</p>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                \n");
        html.append("                <h3>Optimization Tips</h3>\n");
        html.append("                <ul>\n");
        html.append("                    <li><strong>Group similar tests:</strong> Use the same @ContextConfiguration across related test classes</li>\n");
        html.append("                    <li><strong>Minimize @DirtiesContext:</strong> Avoid marking contexts as dirty unless absolutely necessary</li>\n");
        html.append("                    <li><strong>Use test slices:</strong> @WebMvcTest, @DataJpaTest create focused, cacheable contexts</li>\n");
        html.append("                    <li><strong>Consolidate profiles:</strong> Use consistent @ActiveProfiles across test classes</li>\n");
        html.append("                </ul>\n");
        html.append("                \n");
        html.append("                <div class=\"docs-links\">\n");
        html.append("                    <h3>📚 Learn More</h3>\n");
        html.append("                    <p>For detailed information about Spring Test context caching:</p>\n");
        html.append("                    <ul>\n");
        html.append("                        <li><a href=\"https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html\" target=\"_blank\">Context Caching - Spring Framework Reference</a></li>\n");
        html.append("                        <li><a href=\"https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/support-classes.html\" target=\"_blank\">TestContext Framework Support Classes</a></li>\n");
        html.append("                        <li><a href=\"https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html\" target=\"_blank\">Testing Spring Boot Applications</a></li>\n");
        html.append("                        <li><a href=\"https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dirtiescontext.html\" target=\"_blank\">@DirtiesContext Annotation</a></li>\n");
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    private String generateSummarySection() {
        long totalTests = testClassData.stream()
            .mapToLong(TestClassExecutionData::getTotalTests)
            .sum();
        
        long passedTests = testClassData.stream()
            .mapToLong(TestClassExecutionData::getPassedTests)
            .sum();
        
        long failedTests = testClassData.stream()
            .mapToLong(TestClassExecutionData::getFailedTests)
            .sum();
        
        long disabledTests = testClassData.stream()
            .mapToLong(TestClassExecutionData::getDisabledTests)
            .sum();
        
        long abortedTests = testClassData.stream()
            .mapToLong(TestClassExecutionData::getAbortedTests)
            .sum();
        
        long totalTestClasses = testClassData.size();
        
        // Calculate total execution time across all tests
        long totalExecutionTimeMs = testClassData.stream()
            .flatMap(classData -> classData.getTestExecutions().values().stream())
            .filter(testData -> testData.getDuration() != null)
            .mapToLong(testData -> testData.getDuration().toMillis())
            .sum();
        
        StringBuilder html = new StringBuilder();
        html.append("        <h2>Test Execution Summary</h2>\n");
        html.append("        <div class=\"summary-grid\">\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <h3>Test Classes</h3>\n");
        html.append("                <div class=\"value\">").append(totalTestClasses).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <h3>Total Tests</h3>\n");
        html.append("                <div class=\"value\">").append(totalTests).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card passed\">\n");
        html.append("                <h3>Passed</h3>\n");
        html.append("                <div class=\"value\">").append(passedTests).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card failed\">\n");
        html.append("                <h3>Failed</h3>\n");
        html.append("                <div class=\"value\">").append(failedTests).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card disabled\">\n");
        html.append("                <h3>Disabled</h3>\n");
        html.append("                <div class=\"value\">").append(disabledTests).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card aborted\">\n");
        html.append("                <h3>Aborted</h3>\n");
        html.append("                <div class=\"value\">").append(abortedTests).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <h3>Total Runtime</h3>\n");
        html.append("                <div class=\"value\" style=\"font-size: 24px;\">").append(formatDuration(totalExecutionTimeMs)).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"summary-card\">\n");
        html.append("                <h3>Success Rate</h3>\n");
        html.append("                <div class=\"value\" style=\"font-size: 24px; color: ").append(passedTests == totalTests ? "#27ae60" : "#e74c3c").append(";\">")
            .append(totalTests > 0 ? String.format("%.1f%%", (passedTests * 100.0) / totalTests) : "0%").append("</div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    private String generateContextCachingSection() {
        SpringContextStatistics aggregatedStats = aggregateContextStatistics();
        
        StringBuilder html = new StringBuilder();
        html.append("        <h2>Spring Context Caching Statistics</h2>\n");
        html.append("        <div class=\"cache-stats\">\n");
        html.append("            <div class=\"cache-metric\">\n");
        html.append("                <span class=\"label\">Total Context Loads:</span>\n");
        html.append("                <span class=\"value\">").append(aggregatedStats.getContextLoads()).append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"cache-metric\">\n");
        html.append("                <span class=\"label\">Cache Hits:</span>\n");
        html.append("                <span class=\"value\">").append(aggregatedStats.getCacheHits()).append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"cache-metric\">\n");
        html.append("                <span class=\"label\">Cache Misses:</span>\n");
        html.append("                <span class=\"value\">").append(aggregatedStats.getCacheMisses()).append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"cache-metric\">\n");
        html.append("                <span class=\"label\">Cache Hit Rate:</span>\n");
        html.append("                <span class=\"value hit-rate\">").append(String.format("%.1f%%", aggregatedStats.getCacheHitRate())).append("</span>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"cache-metric\">\n");
        html.append("                <span class=\"label\">Total Context Load Time:</span>\n");
        html.append("                <span class=\"value\">").append(formatDuration(aggregatedStats.getTotalContextLoadTime().toMillis())).append("</span>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    private String generateCacheKeysSection() {
        StringBuilder html = new StringBuilder();
        html.append("        <h2>Spring Context Cache Keys</h2>\n");
        html.append("        <div class=\"cache-keys-section\">\n");
        html.append("            <p>This section shows all Spring context cache keys that were used during test execution, along with which test classes utilized each cache key.</p>\n");
        
        // Collect all cache key information from all test classes
        Map<String, Set<String>> allCacheKeys = new HashMap<>();
        Map<String, SpringContextStatistics.CacheKeyInfo> allCacheKeyInfo = new HashMap<>();
        
        for (TestClassExecutionData classData : testClassData) {
            SpringContextStatistics stats = classData.getContextStatistics();
            if (stats != null) {
                Map<String, Set<String>> classCacheKeys = stats.getCacheKeyToTestClasses();
                Map<String, SpringContextStatistics.CacheKeyInfo> classCacheKeyInfo = stats.getCacheKeyInfoMap();
                
                for (Map.Entry<String, Set<String>> entry : classCacheKeys.entrySet()) {
                    String cacheKey = entry.getKey();
                    allCacheKeys.computeIfAbsent(cacheKey, k -> new HashSet<>()).add(classData.getClassName());
                }
                
                for (Map.Entry<String, SpringContextStatistics.CacheKeyInfo> entry : classCacheKeyInfo.entrySet()) {
                    String cacheKey = entry.getKey();
                    SpringContextStatistics.CacheKeyInfo info = entry.getValue();
                    SpringContextStatistics.CacheKeyInfo existing = allCacheKeyInfo.get(cacheKey);
                    if (existing == null) {
                        allCacheKeyInfo.put(cacheKey, new SpringContextStatistics.CacheKeyInfo(cacheKey));
                        existing = allCacheKeyInfo.get(cacheKey);
                    }
                    // Aggregate the statistics
                    for (int i = 0; i < info.getHits(); i++) {
                        existing.incrementHits();
                    }
                    for (int i = 0; i < info.getMisses(); i++) {
                        existing.incrementMisses();
                    }
                }
            }
        }
        
        if (allCacheKeys.isEmpty()) {
            html.append("            <div class=\"no-cache-keys\">\n");
            html.append("                <p>No cache keys were recorded during test execution.</p>\n");
            html.append("            </div>\n");
        } else {
            // Sort cache keys by name for consistent display
            allCacheKeys.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String cacheKey = entry.getKey();
                    Set<String> testClasses = entry.getValue();
                    SpringContextStatistics.CacheKeyInfo keyInfo = allCacheKeyInfo.get(cacheKey);
                    
                    html.append("            <div class=\"cache-key-item\">\n");
                    html.append("                <div class=\"cache-key-name\">🔑 ").append(escapeHtml(cacheKey)).append("</div>\n");
                    
                    if (keyInfo != null) {
                        html.append("                <div class=\"cache-key-usage\">\n");
                        html.append("                    <span class=\"cache-key-stat\">📊 Total Accesses: ").append(keyInfo.getTotalAccesses()).append("</span>\n");
                        html.append("                    <span class=\"cache-key-stat\">🎯 Hits: ").append(keyInfo.getHits()).append("</span>\n");
                        html.append("                    <span class=\"cache-key-stat\">❌ Misses: ").append(keyInfo.getMisses()).append("</span>\n");
                        if (keyInfo.getTotalAccesses() > 0) {
                            html.append("                    <span class=\"cache-key-stat\">📈 Hit Rate: ").append(String.format("%.1f%%", keyInfo.getHitRate())).append("</span>\n");
                        }
                        html.append("                </div>\n");
                    }
                    
                    html.append("                <div class=\"test-classes-list\">\n");
                    html.append("                    <strong>Used by test classes:</strong><br>\n");
                    testClasses.stream()
                        .sorted()
                        .forEach(testClass -> {
                            String simpleClassName = getSimpleClassName(testClass);
                            html.append("                    <span class=\"test-class-tag\">").append(escapeHtml(simpleClassName)).append("</span>\n");
                        });
                    html.append("                </div>\n");
                    html.append("            </div>\n");
                });
        }
        
        html.append("        </div>\n");
        return html.toString();
    }
    
    private String generateTestExecutionSection() {
        StringBuilder html = new StringBuilder();
        html.append("        <h2>Test Execution Details</h2>\n");
        html.append("        <div class=\"execution-overview\">\n");
        html.append("            <p>Click on any test class below to expand and see detailed information about individual test methods, including execution times and failure details.</p>\n");
        html.append("        </div>\n");
        
        // Sort test classes by name for consistent ordering
        testClassData.stream()
            .sorted((a, b) -> a.getClassName().compareTo(b.getClassName()))
            .forEach(classData -> {
                // Calculate class-level execution time
                long classExecutionTimeMs = classData.getTestExecutions().values().stream()
                    .filter(testData -> testData.getDuration() != null)
                    .mapToLong(testData -> testData.getDuration().toMillis())
                    .sum();
                
                // Determine class status color
                String classStatusColor = "#27ae60"; // green for all passed
                if (classData.getFailedTests() > 0) {
                    classStatusColor = "#e74c3c"; // red for any failures
                } else if (classData.getAbortedTests() > 0) {
                    classStatusColor = "#f39c12"; // orange for aborted
                } else if (classData.getDisabledTests() > 0 && classData.getPassedTests() == 0) {
                    classStatusColor = "#95a5a6"; // gray for all disabled
                }
                
                html.append("        <div class=\"test-class\">\n");
                html.append("            <div class=\"test-class-header\" onclick=\"toggleClass(this)\" style=\"border-left: 4px solid ").append(classStatusColor).append(";\">\n");
                html.append("                <div>\n");
                html.append("                    <div class=\"test-class-name\">").append(getSimpleClassName(classData.getClassName())).append("</div>\n");
                html.append("                    <div class=\"test-class-package\">").append(getPackageName(classData.getClassName())).append("</div>\n");
                html.append("                </div>\n");
                html.append("                <div class=\"test-class-stats\">\n");
                html.append("                    <span>⏱️ ").append(formatDuration(classExecutionTimeMs)).append("</span>\n");
                html.append("                    <span>📊 ").append(classData.getTotalTests()).append(" tests</span>\n");
                
                if (classData.getPassedTests() > 0) {
                    html.append("                    <span style=\"color: #27ae60;\">✅ ").append(classData.getPassedTests()).append("</span>\n");
                }
                if (classData.getFailedTests() > 0) {
                    html.append("                    <span style=\"color: #e74c3c;\">❌ ").append(classData.getFailedTests()).append("</span>\n");
                }
                if (classData.getDisabledTests() > 0) {
                    html.append("                    <span style=\"color: #95a5a6;\">⏸️ ").append(classData.getDisabledTests()).append("</span>\n");
                }
                if (classData.getAbortedTests() > 0) {
                    html.append("                    <span style=\"color: #f39c12;\">⚠️ ").append(classData.getAbortedTests()).append("</span>\n");
                }
                
                html.append("                </div>\n");
                html.append("            </div>\n");
                html.append("            <div class=\"test-methods\">\n");
                
                // Add Spring context info for this class
                SpringContextStatistics contextStats = classData.getContextStatistics();
                if (contextStats != null && (contextStats.getContextLoads() > 0 || contextStats.getCacheHits() > 0)) {
                    html.append("                <div class=\"context-info\">\n");
                    html.append("                    <h4>🏗️ Spring Context Information</h4>\n");
                    html.append("                    <div class=\"context-metrics\">\n");
                    if (contextStats.getContextLoads() > 0) {
                        html.append("                        <span class=\"context-metric\">🔄 Context Loads: ").append(contextStats.getContextLoads()).append("</span>\n");
                    }
                    if (contextStats.getCacheHits() > 0) {
                        html.append("                        <span class=\"context-metric\">🎯 Cache Hits: ").append(contextStats.getCacheHits()).append("</span>\n");
                    }
                    if (contextStats.getTotalContextLoadTime().toMillis() > 0) {
                        html.append("                        <span class=\"context-metric\">⏱️ Load Time: ").append(formatDuration(contextStats.getTotalContextLoadTime().toMillis())).append("</span>\n");
                    }
                    html.append("                    </div>\n");
                    html.append("                </div>\n");
                }
                
                // Sort test methods by status (failed first, then others by name)
                classData.getTestExecutions().values().stream()
                    .sorted((a, b) -> {
                        // Failed tests first
                        if (a.getStatus() == TestStatus.FAILED && b.getStatus() != TestStatus.FAILED) return -1;
                        if (b.getStatus() == TestStatus.FAILED && a.getStatus() != TestStatus.FAILED) return 1;
                        // Then by name
                        return a.getTestMethodName().compareTo(b.getTestMethodName());
                    })
                    .forEach(testData -> {
                        String statusClass = testData.getStatus().toString().toLowerCase();
                        String statusIcon = getStatusIcon(testData.getStatus());
                        
                        html.append("                <div class=\"test-method ").append(statusClass).append("\">\n");
                        html.append("                    <div class=\"test-method-header\">\n");
                        html.append("                        <span class=\"test-method-name\">").append(statusIcon).append(" ").append(testData.getTestMethodName()).append("</span>\n");
                        html.append("                        <div class=\"test-method-info\">\n");
                        if (testData.getDuration() != null) {
                            html.append("                            <span class=\"test-duration\">").append(formatDuration(testData.getDuration().toMillis())).append("</span>\n");
                        }
                        html.append("                            <span class=\"test-status\">").append(testData.getStatus().toString()).append("</span>\n");
                        html.append("                        </div>\n");
                        html.append("                    </div>\n");
                        
                        if (testData.getThrowable() != null) {
                            html.append("                    <div class=\"error-details\">\n");
                            html.append("                        <h5>Error Details:</h5>\n");
                            html.append("                        <pre>").append(escapeHtml(getFormattedError(testData.getThrowable()))).append("</pre>\n");
                            html.append("                    </div>\n");
                        }
                        
                        if (testData.getReason() != null && !testData.getReason().trim().isEmpty()) {
                            html.append("                    <div class=\"test-reason\">\n");
                            html.append("                        <strong>Reason:</strong> ").append(escapeHtml(testData.getReason())).append("\n");
                            html.append("                    </div>\n");
                        }
                        
                        html.append("                </div>\n");
                    });
                
                html.append("            </div>\n");
                html.append("        </div>\n");
            });
        
        return html.toString();
    }
    
    private String generateScript() {
        return """
            <script>
                function toggleClass(element) {
                    const methods = element.nextElementSibling;
                    methods.classList.toggle('show');
                }
            </script>
            """;
    }
    
    private SpringContextStatistics aggregateContextStatistics() {
        SpringContextStatistics aggregated = new SpringContextStatistics();
        
        for (TestClassExecutionData classData : testClassData) {
            SpringContextStatistics stats = classData.getContextStatistics();
            if (stats != null) {
                // Aggregate the statistics
                for (int i = 0; i < stats.getContextLoads(); i++) {
                    aggregated.recordContextLoad("aggregated", java.time.Duration.ZERO);
                }
                for (int i = 0; i < stats.getCacheHits(); i++) {
                    aggregated.recordCacheHit("aggregated");
                }
            }
        }
        
        return aggregated;
    }
    
    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            return String.format("%.1fm", millis / 60000.0);
        }
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    private String getPackageName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(0, lastDot) : "";
    }
    
    private String getStatusIcon(TestStatus status) {
        return switch (status) {
            case PASSED -> "✅";
            case FAILED -> "❌";
            case DISABLED -> "⏸️";
            case ABORTED -> "⚠️";
            case RUNNING -> "🔄";
            case PENDING -> "⏳";
        };
    }
    
    private String getFormattedError(Throwable throwable) {
        if (throwable == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getSimpleName()).append(": ");
        if (throwable.getMessage() != null) {
            sb.append(throwable.getMessage());
        }
        
        // Add first few stack trace lines for context
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > 0) {
            sb.append("\n\nStack trace (first 5 lines):");
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                sb.append("\n  at ").append(stackTrace[i].toString());
            }
            if (stackTrace.length > 5) {
                sb.append("\n  ... ").append(stackTrace.length - 5).append(" more");
            }
        }
        
        return sb.toString();
    }
}