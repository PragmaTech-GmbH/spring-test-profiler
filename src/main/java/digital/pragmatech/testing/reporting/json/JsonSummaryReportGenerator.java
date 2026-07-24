package digital.pragmatech.testing.reporting.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.OptimizationStatistics;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import digital.pragmatech.testing.util.SimpleJsonWriter;
import digital.pragmatech.testing.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the flat JSON summary report. Two files are written per run, mirroring the HTML report
 * naming: a timestamped test-profiler-report-[timestamp].json and results.json holding the latest
 * run.
 */
public class JsonSummaryReportGenerator {

  public static final String LATEST_FILE_NAME = "results.json";

  private static final Logger logger = LoggerFactory.getLogger(JsonSummaryReportGenerator.class);

  public void generateSummaryReport(
      Path reportDir,
      String timestamp,
      String buildTool,
      TestExecutionTracker executionTracker,
      SpringContextCacheAccessor.CacheStatistics cacheStats,
      ContextCacheTracker contextCacheTracker) {
    try {
      Files.createDirectories(reportDir);

      JsonSummaryReport summary =
          buildSummary(buildTool, executionTracker, cacheStats, contextCacheTracker);
      String json = SimpleJsonWriter.toJsonPretty(summary);

      Path timestampedFile = reportDir.resolve("test-profiler-report-" + timestamp + ".json");
      Files.writeString(timestampedFile, json, StandardCharsets.UTF_8);

      Path latestFile = reportDir.resolve(LATEST_FILE_NAME);
      Files.writeString(latestFile, json, StandardCharsets.UTF_8);

      logger.info(
          "Spring Test Profiler JSON summary generated: {} (latest: {})",
          timestampedFile.toAbsolutePath(),
          latestFile.toAbsolutePath());
    } catch (IOException e) {
      logger.error("Failed to generate Spring Test Profiler JSON summary report", e);
    }
  }

  JsonSummaryReport buildSummary(
      String buildTool,
      TestExecutionTracker executionTracker,
      SpringContextCacheAccessor.CacheStatistics cacheStats,
      ContextCacheTracker contextCacheTracker) {

    long testsPassed = countMethodsWithStatus(executionTracker, TestStatus.PASSED);
    long testsFailed = countMethodsWithStatus(executionTracker, TestStatus.FAILED);
    long testsDisabled = countMethodsWithStatus(executionTracker, TestStatus.DISABLED);
    long testsAborted = countMethodsWithStatus(executionTracker, TestStatus.ABORTED);

    int contextsCreated = 0;
    int contextCacheHits = 0;
    int contextCacheMisses = 0;
    Integer availableProcessors = null;
    OptimizationStatistics optimizationStats = null;

    if (contextCacheTracker != null) {
      contextsCreated = contextCacheTracker.getTotalContextsCreated();
      contextCacheHits = contextCacheTracker.getCacheHits();
      contextCacheMisses = contextCacheTracker.getCacheMisses();
      optimizationStats = contextCacheTracker.calculateOptimizationStatistics();
      availableProcessors =
          contextCacheTracker.getAllEntries().stream()
              .filter(entry -> entry.getAvailableProcessors() > 0)
              .map(entry -> entry.getAvailableProcessors())
              .findFirst()
              .orElse(null);
    }

    int totalCacheAccesses = contextCacheHits + contextCacheMisses;
    double contextCacheHitRatio =
        totalCacheAccesses > 0
            ? roundTo4Decimals((double) contextCacheHits / totalCacheAccesses)
            : 0.0;

    return new JsonSummaryReport(
        JsonSummaryReport.CURRENT_SCHEMA_VERSION,
        VersionInfo.getVersion(),
        Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
        buildTool,
        executionTracker.getOverallDuration().toMillis(),
        executionTracker.getTotalTestClasses(),
        executionTracker.getTotalTestMethods(),
        testsPassed,
        testsFailed,
        testsDisabled,
        testsAborted,
        contextsCreated,
        contextCacheHits,
        contextCacheMisses,
        contextCacheHitRatio,
        cacheStats != null ? cacheStats.size() : 0,
        cacheStats != null ? cacheStats.maxSize() : 0,
        optimizationStats != null ? optimizationStats.getTotalContextCreationTimeMs() : 0,
        optimizationStats != null ? optimizationStats.getWastedTimeMs() : 0,
        optimizationStats != null ? optimizationStats.getPotentialTimeSavingsMs() : 0,
        optimizationStats != null
            ? roundTo2Decimals(optimizationStats.getPotentialTimeSavingsPercentage())
            : 0.0,
        availableProcessors);
  }

  private long countMethodsWithStatus(TestExecutionTracker executionTracker, TestStatus status) {
    return executionTracker.getClassMetrics().values().stream()
        .flatMap(classMetrics -> classMetrics.getMethodMetrics().values().stream())
        .filter(methodMetrics -> methodMetrics.getStatus() == status)
        .count();
  }

  private double roundTo4Decimals(double value) {
    return Math.round(value * 10_000.0) / 10_000.0;
  }

  private double roundTo2Decimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
