
package digital.pragmatech.testing.reporting.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonReportGenerator {

  private static final Logger logger = LoggerFactory.getLogger(JsonReportGenerator.class);

  private final ObjectMapper objectMapper;

  public JsonReportGenerator() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  public void generateJsonReport(Path reportDir, TestExecutionTracker executionTracker,
    SpringContextCacheAccessor.CacheStatistics cacheStats,
    ContextCacheTracker contextCacheTracker) {
    try {
      Files.createDirectories(reportDir);

      String uniqueId = UUID.randomUUID().toString();
      String jsonFileName = String.format("spring-test-profiler-%s.json", uniqueId);
      Path jsonFile = reportDir.resolve(jsonFileName);

      ReportData reportData = new ReportData(executionTracker, cacheStats, contextCacheTracker);

      objectMapper.writeValue(jsonFile.toFile(), reportData);

      logger.info("Successfully generated JSON report: {}", jsonFile.toAbsolutePath());

    }
    catch (IOException e) {
      logger.error("Failed to generate JSON report", e);
    }
  }

  private record ReportData(
    TestExecutionTracker executionTracker,
    SpringContextCacheAccessor.CacheStatistics cacheStats,
    ContextCacheTracker contextCacheTracker) {
  }
}
