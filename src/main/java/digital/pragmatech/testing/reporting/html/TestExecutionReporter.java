package digital.pragmatech.testing.reporting.html;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.OptimizationStatistics;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TimelineData;
import digital.pragmatech.testing.reporting.TemplateHelpers;
import digital.pragmatech.testing.reporting.json.JsonReportGenerator;
import digital.pragmatech.testing.util.BuildToolDetection;
import digital.pragmatech.testing.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class TestExecutionReporter {

  private static final Logger logger = LoggerFactory.getLogger(TestExecutionReporter.class);
  private static final String REPORT_DIR_NAME = "spring-test-profiler";
  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private final TemplateEngine templateEngine;
  private final JsonReportGenerator jsonReportGenerator;

  public TestExecutionReporter() {
    this.templateEngine = createTemplateEngine();
    this.jsonReportGenerator = new JsonReportGenerator();
  }

  public void generateReport(TestExecutionTracker executionTracker,
    SpringContextCacheAccessor.CacheStatistics cacheStats,
    ContextCacheTracker contextCacheTracker) {

    // Beta feature flag for JSON reporting
    boolean jsonReportingEnabled = Boolean.parseBoolean(System.getProperty("spring.test.insight.json.beta", "false"));

    try {
      BuildToolDetection.BuildTool buildTool = BuildToolDetection.getDetectedBuildTool();
      Path reportDir = determineReportDirectory(buildTool);
      Files.createDirectories(reportDir);

      if (jsonReportingEnabled) {
        jsonReportGenerator.generateJsonReport(reportDir, executionTracker, cacheStats, contextCacheTracker);
      }
      else {
        // Original HTML reporting logic
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String reportFileName = "test-profiler-report-" + timestamp + ".html";
        Path reportFile = reportDir.resolve(reportFileName);

        String htmlContent = generateHtmlWithThymeleaf(buildTool.name(), executionTracker, cacheStats, contextCacheTracker);
        Files.write(reportFile, htmlContent.getBytes());

        logger.info("Spring Test Profiler report generated for {} build tool: {}", buildTool.name(), reportFile.toAbsolutePath());

        // Also create a latest.html symlink for easy access
        Path latestLink = reportDir.resolve("latest.html");
        Files.deleteIfExists(latestLink);
        Files.write(latestLink, htmlContent.getBytes());
      }

    }
    catch (IOException e) {
      logger.error("Failed to generate Spring Test Profiler report", e);
    }
  }

  /**
   * Determines the report directory based on the build tool and system properties.
   * Supports custom directory via system property, or defaults to build tool conventions.
   */
  private Path determineReportDirectory(BuildToolDetection.BuildTool buildTool) {
    String customDir = System.getProperty("pragmatech.spring.test.insight.report.dir");

    if (customDir != null && !customDir.trim().isEmpty()) {
      return Paths.get(customDir);
    }

    String baseDir;

    switch (buildTool) {
      case MAVEN:
        baseDir = "target";
        break;
      case GRADLE:
        baseDir = "build";
        break;
      default:
        // For unknown build tools, try to detect from current directory structure
        if (Files.exists(Paths.get("target"))) {
          baseDir = "target";
        }
        else if (Files.exists(Paths.get("build"))) {
          baseDir = "build";
        }
        else {
          // Fallback to creating in current directory
          baseDir = ".";
        }
    }

    return Paths.get(baseDir, REPORT_DIR_NAME);
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

  private String generateHtmlWithThymeleaf(String buildTool, TestExecutionTracker executionTracker,
    SpringContextCacheAccessor.CacheStatistics cacheStats,
    ContextCacheTracker contextCacheTracker) {
    try {
      Context context = new Context();

      // Basic template variables
      context.setVariable("phase", buildTool);
      context.setVariable("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      context.setVariable("executionTracker", executionTracker);
      context.setVariable("cacheStats", cacheStats);
      context.setVariable("contextCacheTracker", contextCacheTracker);

      // Execution environment info
      context.setVariable("executionEnvironment", "maven");
      context.setVariable("executionTimestamp", LocalDateTime.now());
      context.setVariable("timeZone", ZoneId.systemDefault().getId());

      // Extension version info
      context.setVariable("extensionVersion", VersionInfo.getVersion());

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

      // Calculate and add optimization statistics
      if (contextCacheTracker != null) {
        OptimizationStatistics optimizationStats =
          contextCacheTracker.calculateOptimizationStatistics();
        context.setVariable("optimizationStats", optimizationStats);

        // Add timeline data for visualization
        TimelineData timelineData = contextCacheTracker.getTimelineData();
        context.setVariable("timelineData", timelineData);
      }

      // Load CSS content
      String cssContent = loadCssContent();
      context.setVariable("cssContent", cssContent);

      // Register helper beans for templates
      registerHelperBeans(context, contextCacheTracker);

      String result = templateEngine.process("report", context);
      logger.info("Successfully generated HTML with Thymeleaf templates");
      return result;
    }
    catch (Exception e) {
      logger.error("Failed to generate HTML with Thymeleaf ", e);
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
    context.setVariable("testStatusCounter", new TemplateHelpers.TestStatusCounter());
    context.setVariable("jsonHelper", new TemplateHelpers.JsonHelper());
  }

  private String loadCssContent() {
    try {
      // Use InputStream to read from classpath resource which works both in IDE and JAR
      try (var inputStream = getClass().getClassLoader()
        .getResourceAsStream("static/css/spring-test-profiler.css")) {
        if (inputStream == null) {
          throw new RuntimeException("CSS file not found in classpath: static/css/spring-test-profiler.css");
        }
        return new String(inputStream.readAllBytes());
      }
    }
    catch (Exception e) {
      logger.error("Could not load CSS file. Report generation will fail.", e);
      throw new RuntimeException("CSS file not found", e);
    }
  }
}
