package digital.pragmatech.testing.reporting.html;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionReporterTest {

  private static final String REPORT_DIR_PROPERTY = "pragmatech.spring.test.insight.report.dir";

  @TempDir Path reportDir;

  @AfterEach
  void tearDown() {
    System.clearProperty(REPORT_DIR_PROPERTY);
  }

  @Test
  void shouldGenerateHtmlReportAndJsonSummaryTogether() throws IOException {
    System.setProperty(REPORT_DIR_PROPERTY, reportDir.toString());

    TestExecutionTracker executionTracker = new TestExecutionTracker();
    executionTracker.startTracking();
    executionTracker.recordTestClassStart("com.example.CheckoutTest");
    executionTracker.recordTestMethodStart("com.example.CheckoutTest", "shouldCheckout");
    executionTracker.recordTestMethodEnd(
        "com.example.CheckoutTest", "shouldCheckout", TestStatus.PASSED);
    executionTracker.recordTestClassEnd("com.example.CheckoutTest");
    executionTracker.stopTracking();

    SpringContextCacheAccessor.CacheStatistics cacheStats =
        new SpringContextCacheAccessor.CacheStatistics(1, 2, 1, 32, List.of("contextKey"));

    new TestExecutionReporter()
        .generateReport(executionTracker, cacheStats, new ContextCacheTracker());

    assertThat(reportDir.resolve("latest.html")).exists();
    assertThat(reportDir.resolve("results.json")).exists();

    try (var files = Files.list(reportDir)) {
      List<String> fileNames = files.map(path -> path.getFileName().toString()).toList();
      assertThat(fileNames)
          .anyMatch(name -> name.matches("test-profiler-report-.+\\.html"))
          .anyMatch(name -> name.matches("test-profiler-report-.+\\.json"));
    }

    String jsonContent = Files.readString(reportDir.resolve("results.json"));
    assertThat(jsonContent)
        .contains("\"schemaVersion\": 1")
        .contains("\"totalDurationMs\"")
        .contains("\"springContextCacheMaxSize\": 32");
  }
}
