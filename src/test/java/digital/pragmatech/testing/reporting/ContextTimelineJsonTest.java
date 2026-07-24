package digital.pragmatech.testing.reporting;

import java.time.Instant;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.ContextRemovalReason;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTimelineJsonTest {

  private ContextCacheTracker contextCacheTracker;
  private TestExecutionTracker executionTracker;
  private TemplateHelpers.JsonHelper jsonHelper;

  @BeforeEach
  void setUp() {
    contextCacheTracker = new ContextCacheTracker();
    executionTracker = new TestExecutionTracker();
    jsonHelper = new TemplateHelpers.JsonHelper();
  }

  @Test
  void shouldReturnEmptyObjectForNullTracker() {
    assertEquals("{}", jsonHelper.contextTimelineToJson(null, executionTracker, 32));
  }

  @Test
  void shouldEmitEmptyContextsArrayWhenNothingWasCreated() {
    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    assertTrue(json.contains("\"contexts\":[]"), json);
    assertTrue(json.contains("\"maxCacheSize\":32"), json);
  }

  @Test
  void shouldEmitMillisecondPrecisionSegments() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 1234);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    assertTrue(json.contains("\"testRunStartMs\":"), json);
    assertTrue(json.contains("\"testRunEndMs\":"), json);
    assertTrue(json.contains("\"segments\":["), json);
    assertTrue(json.contains("\"loadMs\":1234"), json);
    // Open segment: no removal recorded
    assertTrue(json.contains("\"removedMs\":null"), json);
    assertTrue(json.contains("\"removalReason\":null"), json);

    // Millisecond precision: startMs must be 13 digits (epoch millis), not 10 (epoch seconds)
    String startMsValue = json.replaceAll(".*\"startMs\":(\\d+).*", "$1");
    assertEquals(13, startMsValue.length(), "startMs should be epoch milliseconds: " + json);
  }

  @Test
  void shouldEmitRemovalTimeAndReason() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 500);
    contextCacheTracker.recordContextRemoval(
        config, Instant.now(), ContextRemovalReason.DIRTIES_CONTEXT);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    assertTrue(json.contains("\"removalReason\":\"DIRTIES_CONTEXT\""), json);
    String removedMsValue = json.replaceAll(".*\"removedMs\":(\\d+).*", "$1");
    assertEquals(13, removedMsValue.length(), "removedMs should be epoch milliseconds: " + json);
  }

  @Test
  void shouldEmitOneSegmentPerLifespan() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 500);
    contextCacheTracker.recordContextRemoval(
        config, Instant.now(), ContextRemovalReason.DIRTIES_CONTEXT);
    contextCacheTracker.recordContextCreation(config, 300);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    ContextCacheEntry entry = contextCacheTracker.getCacheEntry(config).orElseThrow();
    assertEquals(2, entry.getLifespans().size());
    assertTrue(json.contains("\"loadMs\":500"), json);
    assertTrue(json.contains("\"loadMs\":300"), json);
    // The second lifespan is still open
    assertTrue(json.contains("\"removedMs\":null"), json);
  }

  @Test
  void shouldEmitTestExecutionsForContext() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 500);
    contextCacheTracker.recordTestMethodForContext(config, "com.example.TestA", "shouldWork");
    executionTracker.recordTestClassStart("com.example.TestA");
    executionTracker.recordTestMethodStart("com.example.TestA", "shouldWork");
    executionTracker.recordTestMethodEnd("com.example.TestA", "shouldWork", TestStatus.PASSED);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    assertTrue(json.contains("\"testExecutions\":["), json);
    assertTrue(json.contains("\"testClass\":\"com.example.TestA\""), json);
    assertTrue(json.contains("\"testMethod\":\"shouldWork\""), json);
    assertTrue(json.contains("\"status\":\"PASSED\""), json);
  }

  @Test
  void shouldEmitEmptyTestExecutionsWithoutExecutionTracker() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 500);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, null, 32);

    assertTrue(json.contains("\"testExecutions\":[]"), json);
  }

  @Test
  void shouldContainContextMetadata() {
    executionTracker.startTracking();
    MergedContextConfiguration config = createConfig(Object.class);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestA");
    contextCacheTracker.recordContextCreation(config, 500);
    executionTracker.stopTracking();

    String json = jsonHelper.contextTimelineToJson(contextCacheTracker, executionTracker, 32);

    assertTrue(json.contains("\"contextKey\":\"context-"), json);
    assertTrue(json.contains("\"testClassCount\":1"), json);
    assertTrue(json.contains("com.example.TestA"), json);
    assertTrue(json.contains("\"beanCount\":"), json);
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
