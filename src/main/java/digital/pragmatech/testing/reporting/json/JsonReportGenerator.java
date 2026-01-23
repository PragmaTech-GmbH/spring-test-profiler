package digital.pragmatech.testing.reporting.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.util.SimpleJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonReportGenerator {

  private static final Logger logger = LoggerFactory.getLogger(JsonReportGenerator.class);

  public void generateJsonReport(
      Path reportDir,
      TestExecutionTracker executionTracker,
      SpringContextCacheAccessor.CacheStatistics cacheStats,
      ContextCacheTracker contextCacheTracker) {
    try {
      Files.createDirectories(reportDir);

      String uniqueId = UUID.randomUUID().toString();
      String jsonFileName = String.format("spring-test-profiler-%s.json", uniqueId);
      Path jsonFile = reportDir.resolve(jsonFileName);

      ReportData reportData = new ReportData(executionTracker, cacheStats, contextCacheTracker);

      String json = SimpleJsonWriter.toJsonPretty(reportData);
      Files.writeString(jsonFile, json, StandardCharsets.UTF_8);

      logger.info("Successfully generated JSON report: {}", jsonFile.toAbsolutePath());

    } catch (IOException e) {
      logger.error("Failed to generate JSON report", e);
    }
  }

  private record ReportData(
      TestExecutionTracker executionTracker,
      SpringContextCacheAccessor.CacheStatistics cacheStats,
      ContextCacheTracker contextCacheTracker) {}
}
