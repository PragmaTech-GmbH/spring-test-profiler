package digital.pragmatech.testing.reporting.html;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import digital.pragmatech.testing.util.BuildToolDetection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    String expectedBuildTool =
        BuildToolDetection.getDetectedBuildTool().name().toLowerCase(Locale.ROOT);
    assertExecutedVia(Files.readString(reportDir.resolve("latest.html")), expectedBuildTool);

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

  @Test
  void shouldAcceptExecutionMetadataFormattingChanges() {
    assertExecutedVia(
        "<strong class=\"label\">  Executed via: \n</strong>\n\t"
            + "<span class=\"value\"> \n gradle \t</span>",
        "gradle");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<strong>Executed via:</strong><span>maven</span><p>gradle</p>",
        "<span>gradle</span>",
        "<strong>Executed via:</strong>",
        "<strong>Executed via:</strong><div>other</div><span>gradle</span>",
        "<strong>Executed via:</strong><span>gradle</span>"
            + "<strong>Executed via:</strong><span>gradle</span>"
      })
  void shouldRejectIncorrectOrAmbiguousExecutionMetadata(String html) {
    assertThrows(AssertionError.class, () -> assertExecutedVia(html, "gradle"));
  }

  private static void assertExecutedVia(String html, String expectedBuildTool) {
    // Match the generated summary's label and adjacent value, allowing attributes and whitespace.
    Pattern executedVia =
        Pattern.compile(
            "<strong(?:\\s+[^>]*)?>\\s*Executed\\s+via:\\s*</strong>\\s*"
                + "<span(?:\\s+[^>]*)?>([^<]*)</span>");
    assertThat(executedVia.matcher(html).results().map(match -> match.group(1).strip()).toList())
        .as("build tool next to the Executed via label")
        .containsExactly(expectedBuildTool);
  }
}
