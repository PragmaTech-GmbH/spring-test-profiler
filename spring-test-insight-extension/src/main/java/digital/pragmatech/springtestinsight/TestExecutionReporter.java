package digital.pragmatech.springtestinsight;

import digital.pragmatech.springtestinsight.pdf.PdfConfiguration;
import digital.pragmatech.springtestinsight.pdf.PdfReportGenerator;
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
    private final PdfReportGenerator pdfGenerator;
    
    public TestExecutionReporter() {
        this.templateEngine = createTemplateEngine();
        this.pdfGenerator = new PdfReportGenerator();
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
            
            // Generate PDF report if enabled
            generatePdfReportIfEnabled(phase, htmlContent, reportDir, timestamp);
            
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
            registerHelperBeans(context);
            
            String result = templateEngine.process("report", context);
            logger.info("Successfully generated HTML with Thymeleaf templates");
            return result;
        } catch (Exception e) {
            logger.error("Failed to generate HTML with Thymeleaf. Error: " + e.getMessage(), e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    
    private void registerHelperBeans(Context context) {
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
        context.setVariable("configurationHelper", new TemplateHelpers.ConfigurationHelper());
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
    
    /**
     * Generates PDF report if PDF generation is enabled in configuration.
     */
    private void generatePdfReportIfEnabled(String phase, String htmlContent, Path reportDir, String timestamp) {
        if (!pdfGenerator.getConfiguration().isPdfEnabled()) {
            logger.debug("PDF generation is disabled, skipping PDF report");
            return;
        }
        
        try {
            // Create PDF filename
            String pdfFileName = phase.equals("default") ? 
                "test-insight-report-" + timestamp + ".pdf" :
                "test-insight-report-" + phase + "-" + timestamp + ".pdf";
            Path pdfFile = reportDir.resolve(pdfFileName);
            
            // Generate PDF with enhanced HTML for print
            String pdfHtmlContent = enhanceHtmlForPdf(htmlContent);
            
            // Debug: Save the enhanced HTML to inspect issues
            try {
                Path debugHtmlFile = reportDir.resolve("debug-pdf-content.html");
                Files.write(debugHtmlFile, pdfHtmlContent.getBytes());
                logger.debug("Saved debug PDF HTML content to: {}", debugHtmlFile);
            } catch (Exception debugEx) {
                logger.debug("Could not save debug HTML file", debugEx);
            }
            
            pdfGenerator.generatePdfReport(pdfHtmlContent, pdfFile);
            
            // Also create a latest PDF file for easy access
            String latestPdfFileName = phase.equals("default") ? "latest.pdf" : "latest-" + phase + ".pdf";
            Path latestPdfLink = reportDir.resolve(latestPdfFileName);
            Files.deleteIfExists(latestPdfLink);
            pdfGenerator.generatePdfReport(pdfHtmlContent, latestPdfLink);
            
            logger.info("PDF report generated for {} phase: {}", phase, pdfFile.toAbsolutePath());
            
        } catch (Exception e) {
            logger.warn("Failed to generate PDF report for {} phase: {}", phase, e.getMessage());
            logger.debug("PDF generation error details", e);
        }
    }
    
    /**
     * Enhances HTML content for PDF generation by replacing the CSS with PDF-specific styling.
     */
    private String enhanceHtmlForPdf(String htmlContent) {
        try {
            // Load PDF-specific CSS using InputStream to work in JAR files
            String pdfCss;
            try (var inputStream = getClass().getClassLoader()
                    .getResourceAsStream("static/css/spring-test-insight-pdf.css")) {
                if (inputStream == null) {
                    logger.warn("PDF CSS file not found in classpath");
                    return htmlContent;
                }
                pdfCss = new String(inputStream.readAllBytes());
            }
            
            // Find the style tag using a more robust approach
            int styleStart = htmlContent.indexOf("<style");
            int styleEnd = htmlContent.indexOf("</style>") + "</style>".length();
            
            if (styleStart != -1 && styleEnd > styleStart) {
                // Replace the entire style block with PDF CSS
                String enhancedHtml = htmlContent.substring(0, styleStart) +
                    "<style type=\"text/css\">\n" + pdfCss + "\n</style>" +
                    htmlContent.substring(styleEnd);
                
                // Ensure proper XHTML compliance for Flying Saucer
                enhancedHtml = ensureXhtmlCompliance(enhancedHtml);
                
                return enhancedHtml;
            } else {
                logger.warn("Could not find style tag in HTML content for PDF generation");
                return htmlContent;
            }
            
        } catch (Exception e) {
            logger.warn("Could not load PDF CSS, using original HTML content", e);
            return htmlContent;
        }
    }
    
    /**
     * Ensures HTML is XHTML compliant for Flying Saucer PDF generation.
     */
    private String ensureXhtmlCompliance(String html) {
        // Flying Saucer requires well-formed XHTML
        String enhanced = html;
        
        // Ensure proper DOCTYPE for XHTML
        if (enhanced.contains("<!DOCTYPE html>")) {
            enhanced = enhanced.replace("<!DOCTYPE html>", 
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        }
        
        // Remove favicon link first as it contains characters that break XML parsing
        // Remove entire lines containing favicon links to avoid XML parsing issues with data URLs
        enhanced = enhanced.replaceAll("(?m)^.*<link[^>]*rel=[\"']icon[\"'].*$", "");
        
        // Ensure html tag has xmlns attribute - use a more robust approach
        if (!enhanced.contains("xmlns=")) {
            // Find the html tag and add xmlns attribute
            int htmlTagStart = enhanced.indexOf("<html");
            if (htmlTagStart != -1) {
                int htmlTagEnd = enhanced.indexOf(">", htmlTagStart);
                if (htmlTagEnd != -1) {
                    // Replace the html tag completely with a properly formatted one
                    String newHtmlTag = "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">";
                    enhanced = enhanced.substring(0, htmlTagStart) + newHtmlTag + enhanced.substring(htmlTagEnd + 1);
                }
            }
        }
        
        // Close self-closing tags properly for XHTML compliance
        enhanced = enhanced.replace("<meta charset=\"UTF-8\">", "<meta charset=\"UTF-8\" />");
        enhanced = enhanced.replaceAll("<meta ([^>]*[^/])>", "<meta $1 />");
        enhanced = enhanced.replace("<br>", "<br />");
        enhanced = enhanced.replaceAll("<br([^>]*[^/])>", "<br$1 />");
        
        // Remove any script tags as they're not needed for PDF and can cause issues
        enhanced = enhanced.replaceAll("<script[^>]*>.*?</script>", "");
        
        // Escape any unescaped ampersands that aren't part of valid entities
        enhanced = enhanced.replaceAll("&(?![a-zA-Z0-9#]+;)", "&amp;");
        
        return enhanced;
    }
    
}