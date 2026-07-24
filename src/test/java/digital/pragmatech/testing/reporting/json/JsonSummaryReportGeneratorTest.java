package digital.pragmatech.testing.reporting.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.SpringContextCacheAccessor;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSummaryReportGeneratorTest {

  private JsonSummaryReportGenerator generator;
  private TestExecutionTracker executionTracker;
  private ContextCacheTracker contextCacheTracker;

  @TempDir Path reportDir;

  @BeforeEach
  void setUp() {
    generator = new JsonSummaryReportGenerator();
    executionTracker = new TestExecutionTracker();
    contextCacheTracker = new ContextCacheTracker();
  }

  @Test
  void shouldWriteTimestampedFileAndLatestResultsJsonWithSameContent() throws IOException {
    executionTracker.startTracking();
    executionTracker.stopTracking();

    generator.generateSummaryReport(
        reportDir, "2026-07-23_10-00-00", "MAVEN", executionTracker, null, contextCacheTracker);

    Path timestampedFile = reportDir.resolve("test-profiler-report-2026-07-23_10-00-00.json");
    Path latestFile = reportDir.resolve("results.json");

    assertThat(timestampedFile).exists();
    assertThat(latestFile).exists();
    assertThat(Files.readString(latestFile)).isEqualTo(Files.readString(timestampedFile));
  }

  @Test
  void shouldOverwriteResultsJsonOnSubsequentRuns() throws IOException {
    generator.generateSummaryReport(
        reportDir, "2026-07-23_10-00-00", "MAVEN", executionTracker, null, contextCacheTracker);

    recordTestMethod("com.example.CheckoutTest", "shouldCheckout", TestStatus.PASSED);
    generator.generateSummaryReport(
        reportDir, "2026-07-23_11-00-00", "MAVEN", executionTracker, null, contextCacheTracker);

    String latestContent = Files.readString(reportDir.resolve("results.json"));
    assertThat(latestContent).contains("\"totalTestMethods\": 1");
    assertThat(reportDir.resolve("test-profiler-report-2026-07-23_10-00-00.json")).exists();
    assertThat(reportDir.resolve("test-profiler-report-2026-07-23_11-00-00.json")).exists();
  }

  @Test
  void shouldReportTestCountsPerStatus() {
    executionTracker.startTracking();
    recordTestMethod("com.example.CheckoutTest", "shouldCheckout", TestStatus.PASSED);
    recordTestMethod("com.example.CheckoutTest", "shouldApplyDiscount", TestStatus.PASSED);
    recordTestMethod("com.example.PaymentTest", "shouldFailOnInvalidCard", TestStatus.FAILED);
    recordTestMethod("com.example.PaymentTest", "shouldSkipLegacyFlow", TestStatus.DISABLED);
    recordTestMethod("com.example.PaymentTest", "shouldAbortWithoutBroker", TestStatus.ABORTED);
    executionTracker.stopTracking();

    JsonSummaryReport summary =
        generator.buildSummary("MAVEN", executionTracker, null, contextCacheTracker);

    assertThat(summary.totalTestClasses()).isEqualTo(2);
    assertThat(summary.totalTestMethods()).isEqualTo(5);
    assertThat(summary.testsPassed()).isEqualTo(2);
    assertThat(summary.testsFailed()).isEqualTo(1);
    assertThat(summary.testsDisabled()).isEqualTo(1);
    assertThat(summary.testsAborted()).isEqualTo(1);
    assertThat(summary.totalDurationMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldReportContextCacheMetrics() {
    MergedContextConfiguration firstContext = createConfig(Object.class);
    MergedContextConfiguration secondContext = createConfig(String.class);

    contextCacheTracker.recordTestClassForContext(firstContext, "com.example.TestA");
    contextCacheTracker.recordContextCreation(firstContext, 1500);
    contextCacheTracker.recordTestClassForContext(secondContext, "com.example.TestB");
    contextCacheTracker.recordContextCreation(secondContext, 500);
    contextCacheTracker.recordTestClassForContext(firstContext, "com.example.TestC");
    contextCacheTracker.recordContextCacheHit(firstContext);
    contextCacheTracker.recordTestClassForContext(secondContext, "com.example.TestD");
    contextCacheTracker.recordContextCacheHit(secondContext);

    JsonSummaryReport summary =
        generator.buildSummary("GRADLE", executionTracker, null, contextCacheTracker);

    assertThat(summary.contextsCreated()).isEqualTo(2);
    assertThat(summary.contextCacheHits()).isEqualTo(2);
    assertThat(summary.contextCacheMisses()).isEqualTo(2);
    assertThat(summary.contextCacheHitRatio()).isEqualTo(0.5);
    assertThat(summary.totalContextCreationTimeMs()).isEqualTo(2000);
    // Load time above the 1s threshold counts as wasted: 1500ms - 1000ms
    assertThat(summary.wastedContextTimeMs()).isEqualTo(500);
  }

  @Test
  void shouldReportSpringCacheStatistics() {
    SpringContextCacheAccessor.CacheStatistics cacheStats =
        new SpringContextCacheAccessor.CacheStatistics(3, 40, 4, 32, List.of("key1", "key2"));

    JsonSummaryReport summary =
        generator.buildSummary("MAVEN", executionTracker, cacheStats, contextCacheTracker);

    assertThat(summary.springContextCacheSize()).isEqualTo(3);
    assertThat(summary.springContextCacheMaxSize()).isEqualTo(32);
  }

  @Test
  void shouldHandleMissingTrackersGracefully() throws IOException {
    generator.generateSummaryReport(
        reportDir, "2026-07-23_10-00-00", "UNKNOWN", executionTracker, null, null);

    String content = Files.readString(reportDir.resolve("results.json"));
    assertThat(content).contains("\"contextsCreated\": 0");
    assertThat(content).contains("\"springContextCacheSize\": 0");
    assertThat(content).contains("\"contextCacheHitRatio\": 0.0");
    assertThat(content).contains("\"availableProcessors\": null");
  }

  @Test
  void shouldWriteFlatSchemaWithStableKeys() throws IOException {
    executionTracker.startTracking();
    recordTestMethod("com.example.CheckoutTest", "shouldCheckout", TestStatus.PASSED);
    executionTracker.stopTracking();

    generator.generateSummaryReport(
        reportDir, "2026-07-23_10-00-00", "MAVEN", executionTracker, null, contextCacheTracker);

    String content = Files.readString(reportDir.resolve("results.json"));
    assertThat(content)
        .contains("\"schemaVersion\": 1")
        .contains("\"profilerVersion\"")
        .contains("\"generatedAt\"")
        .contains("\"buildTool\": \"MAVEN\"")
        .contains("\"totalDurationMs\"")
        .contains("\"totalTestClasses\": 1")
        .contains("\"totalTestMethods\": 1")
        .contains("\"testsPassed\": 1")
        .contains("\"testsFailed\": 0")
        .contains("\"contextsCreated\": 0")
        .contains("\"totalContextCreationTimeMs\": 0");
    // Flat schema: no nested objects or arrays
    assertThat(content).doesNotContain("[");
    assertThat(content.substring(1)).doesNotContain("{");
  }

  private void recordTestMethod(String className, String methodName, TestStatus status) {
    executionTracker.recordTestClassStart(className);
    executionTracker.recordTestMethodStart(className, methodName);
    executionTracker.recordTestMethodEnd(className, methodName, status);
    executionTracker.recordTestClassEnd(className);
  }

  private MergedContextConfiguration createConfig(Class<?>... classes) {
    return new MergedContextConfiguration(
        classes[0],
        null,
        classes,
        null,
        new String[0],
        new String[0],
        null,
        null,
        null,
        null,
        null);
  }
}
