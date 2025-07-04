package digital.pragmatech.springtestinsight;

import digital.pragmatech.springtestinsight.reporting.TemplateHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestExecutionReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionReporter.class);
    private static final String REPORT_DIR_NAME = "spring-test-insight";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final TemplateEngine templateEngine;
    
    public TestExecutionReporter() {
        this.templateEngine = createTemplateEngine();
    }
    
    public void generateReport(String phase, TestExecutionTracker executionTracker, SpringContextCacheAccessor.CacheStatistics cacheStats) {
        generateReport(phase, executionTracker, cacheStats, null);
    }
    
    public void generateReport(String phase, TestExecutionTracker executionTracker, 
                             SpringContextCacheAccessor.CacheStatistics cacheStats, 
                             ContextCacheTracker contextCacheTracker) {
        try {
            Path reportDir = determineReportDirectory();
            Files.createDirectories(reportDir);
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String reportFileName = phase.equals("default") ? 
                "test-insight-report-" + timestamp + ".html" :
                "test-insight-report-" + phase + "-" + timestamp + ".html";
            Path reportFile = reportDir.resolve(reportFileName);
            
            String htmlContent = generateHtmlWithThymeleaf(phase, executionTracker, cacheStats, contextCacheTracker);
            Files.write(reportFile, htmlContent.getBytes());
            
            logger.info("Spring Test Insight report generated for {} phase: {}", phase, reportFile.toAbsolutePath());
            
            // Also create a latest.html symlink for easy access
            String latestFileName = phase.equals("default") ? "latest.html" : "latest-" + phase + ".html";
            Path latestLink = reportDir.resolve(latestFileName);
            Files.deleteIfExists(latestLink);
            Files.write(latestLink, htmlContent.getBytes());
            
            
        } catch (IOException e) {
            logger.error("Failed to generate Spring Test Insight report", e);
        }
    }
    
    /**
     * Determines the report directory based on the build tool and system properties.
     * Supports custom directory via system property, or defaults to build tool conventions.
     */
    private Path determineReportDirectory() {
        // First check if user specified a custom directory
        String customDir = System.getProperty("pragmatech.spring.test.insight.report.dir");
        if (customDir != null && !customDir.trim().isEmpty()) {
            return Paths.get(customDir);
        }
        
        // Otherwise, detect build tool and use appropriate directory
        String buildTool = detectBuildTool();
        String baseDir;
        
        switch (buildTool) {
            case "maven":
                baseDir = "target";
                break;
            case "gradle":
                baseDir = "build";
                break;
            default:
                // For unknown build tools, try to detect from current directory structure
                if (Files.exists(Paths.get("target"))) {
                    baseDir = "target";
                } else if (Files.exists(Paths.get("build"))) {
                    baseDir = "build";
                } else {
                    // Fallback to creating in current directory
                    baseDir = ".";
                }
        }
        
        return Paths.get(baseDir, REPORT_DIR_NAME);
    }
    
    /**
     * Detects the build tool being used based on system properties and classpath indicators.
     * This method is duplicated from SpringTestInsightExtension for independence.
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
     * Detects the execution environment (IntelliJ, Maven Surefire, Failsafe, Gradle, etc.)
     */
    private String detectExecutionEnvironment() {
        // Check for IntelliJ IDEA
        if (System.getProperty("idea.test.cyclic.buffer.size") != null ||
            System.getProperty("idea.launcher.port") != null ||
            System.getProperty("idea.launcher.bin.path") != null ||
            System.getProperty("java.class.path", "").contains("idea_rt.jar")) {
            return "IntelliJ IDEA";
        }
        
        // Check for Eclipse
        if (System.getProperty("eclipse.launcher") != null ||
            System.getProperty("osgi.instance.area") != null ||
            System.getProperty("java.class.path", "").contains("eclipse")) {
            return "Eclipse";
        }
        
        // Check for VS Code
        if (System.getProperty("java.class.path", "").contains("vscode")) {
            return "VS Code";
        }
        
        // Check for Maven Surefire
        if (System.getProperty("surefire.test.class.path") != null ||
            System.getProperty("maven.test.skip") != null ||
            System.getProperty("java.class.path", "").contains("surefire")) {
            return "Maven Surefire";
        }
        
        // Check for Maven Failsafe
        if (System.getProperty("failsafe.test.class.path") != null ||
            System.getProperty("java.class.path", "").contains("failsafe")) {
            return "Maven Failsafe";
        }
        
        // Check for Gradle Test
        if (System.getProperty("org.gradle.test.worker") != null ||
            System.getProperty("gradle.test.ignoreFailures") != null ||
            System.getProperty("java.class.path", "").contains("gradle")) {
            return "Gradle Test";
        }
        
        // Check for generic Maven (fallback)
        if (detectBuildTool().equals("maven")) {
            return "Maven";
        }
        
        // Check for generic Gradle (fallback)
        if (detectBuildTool().equals("gradle")) {
            return "Gradle";
        }
        
        // Default to unknown
        return "Unknown";
    }
    
    private TemplateEngine createTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setCacheable(false); // For development; set to true in production
        resolver.setCharacterEncoding("UTF-8");
        
        engine.setTemplateResolver(resolver);
        return engine;
    }
    
    private String generateHtmlWithThymeleaf(String phase, TestExecutionTracker executionTracker, SpringContextCacheAccessor.CacheStatistics cacheStats) {
        return generateHtmlWithThymeleaf(phase, executionTracker, cacheStats, null);
    }
    
    private String generateHtmlWithThymeleaf(String phase, TestExecutionTracker executionTracker, 
                                           SpringContextCacheAccessor.CacheStatistics cacheStats,
                                           ContextCacheTracker contextCacheTracker) {
        try {
            Context context = new Context();
            
            // Basic template variables
            context.setVariable("phase", phase);
            context.setVariable("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            context.setVariable("executionTracker", executionTracker);
            context.setVariable("cacheStats", cacheStats);
            context.setVariable("contextCacheTracker", contextCacheTracker);
            
            // Execution environment info
            context.setVariable("executionEnvironment", detectExecutionEnvironment());
            context.setVariable("executionTimestamp", LocalDateTime.now());
            context.setVariable("timeZone", ZoneId.systemDefault().getId());
            
            // Pre-compute test status counts to avoid complex template expressions
            Map<String, TestExecutionTracker.TestClassMetrics> classMetrics = executionTracker.getClassMetrics();
            long passedTests = TemplateHelpers.countTestsByStatus(classMetrics, "PASSED");
            long failedTests = TemplateHelpers.countTestsByStatus(classMetrics, "FAILED");
            long disabledTests = TemplateHelpers.countTestsByStatus(classMetrics, "DISABLED");
            long abortedTests = TemplateHelpers.countTestsByStatus(classMetrics, "ABORTED");
            
            context.setVariable("passedTests", passedTests);
            context.setVariable("failedTests", failedTests);
            context.setVariable("disabledTests", disabledTests);
            context.setVariable("abortedTests", abortedTests);
            
            // Pre-compute success rate
            int totalTestMethods = executionTracker.getTotalTestMethods();
            double successRate = totalTestMethods > 0 ? (passedTests * 100.0) / totalTestMethods : 0.0;
            context.setVariable("successRate", successRate);
            
            // Load CSS content
            String cssContent = loadCssContent();
            context.setVariable("cssContent", cssContent);
            
            // Register helper beans for templates
            registerHelperBeans(context, contextCacheTracker);
            
            String result = templateEngine.process("report", context);
            logger.info("Successfully generated HTML with Thymeleaf templates");
            return result;
        } catch (Exception e) {
            logger.error("Failed to generate HTML with Thymeleaf. Error: " + e.getMessage(), e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    
    private void registerHelperBeans(Context context, ContextCacheTracker contextCacheTracker) {
        // Register all helper beans that templates can use
        context.setVariable("durationFormatter", new TemplateHelpers.DurationFormatter());
        context.setVariable("classNameHelper", new TemplateHelpers.ClassNameHelper());
        context.setVariable("statusColorHelper", new TemplateHelpers.StatusColorHelper());
        context.setVariable("statusIconHelper", new TemplateHelpers.StatusIconHelper());
        context.setVariable("errorFormatter", new TemplateHelpers.ErrorFormatter());
        context.setVariable("testMethodSorter", new TemplateHelpers.TestMethodSorter());
        context.setVariable("testClassSorter", new TemplateHelpers.TestClassSorter());
        context.setVariable("classNameComparator", new TemplateHelpers.ClassNameComparator());
        context.setVariable("cacheKeyProcessor", new TemplateHelpers.CacheKeyProcessor());
        context.setVariable("summaryCalculator", new TemplateHelpers.SummaryCalculator());
        context.setVariable("configurationHelper", new TemplateHelpers.ConfigurationHelper(contextCacheTracker));
        context.setVariable("contextConfigurationDetector", ContextConfigurationDetector.class);
        context.setVariable("testStatusCounter", new TemplateHelpers.TestStatusCounter());
    }
    
    private String loadCssContent() {
        try {
            // Use InputStream to read from classpath resource which works both in IDE and JAR
            try (var inputStream = getClass().getClassLoader()
                    .getResourceAsStream("static/css/spring-test-insight.css")) {
                if (inputStream == null) {
                    throw new RuntimeException("CSS file not found in classpath: static/css/spring-test-insight.css");
                }
                return new String(inputStream.readAllBytes());
            }
        } catch (Exception e) {
            logger.error("Could not load CSS file. Report generation will fail.", e);
            throw new RuntimeException("CSS file not found", e);
        }
    }
    
    
    
    
}