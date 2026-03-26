package digital.pragmatech.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextCacheTrackerTest {

  private ContextCacheTracker tracker;

  @BeforeEach
  void setUp() {
    tracker = new ContextCacheTracker();
  }

  @Test
  void shouldReturnZerosWhenNoContextsTracked() {
    assertEquals(0, tracker.getCacheHits());
    assertEquals(0, tracker.getCacheMisses());
    assertEquals(0, tracker.getTotalContextsCreated());
    assertEquals(0.0, tracker.getContextReuseRate());
  }

  @Test
  void shouldTrackSingleContextCreationAsMiss() {
    MergedContextConfiguration config = createConfig(Object.class);

    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);

    assertEquals(0, tracker.getCacheHits());
    assertEquals(1, tracker.getCacheMisses());
    assertEquals(1, tracker.getTotalContextsCreated());
    assertEquals(0.0, tracker.getContextReuseRate());
  }

  @Test
  void shouldTrackCacheHitsWhenContextReused() {
    MergedContextConfiguration config = createConfig(Object.class);

    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);

    tracker.recordTestClassForContext(config, "com.example.TestB");
    tracker.recordContextCacheHit(config);

    tracker.recordTestClassForContext(config, "com.example.TestC");
    tracker.recordContextCacheHit(config);

    assertEquals(2, tracker.getCacheHits());
    assertEquals(1, tracker.getCacheMisses());
    assertEquals(1, tracker.getTotalContextsCreated());
    assertEquals(66.66, tracker.getContextReuseRate(), 0.01);
  }

  @Test
  void shouldTrackMultipleContexts() {
    MergedContextConfiguration config1 = createConfig(Object.class);
    MergedContextConfiguration config2 = createConfig(String.class);

    // First context: 1 miss + 1 hit
    tracker.recordTestClassForContext(config1, "com.example.TestA");
    tracker.recordContextCreation(config1, 500);
    tracker.recordTestClassForContext(config1, "com.example.TestB");
    tracker.recordContextCacheHit(config1);

    // Second context: 1 miss + 1 hit
    tracker.recordTestClassForContext(config2, "com.example.TestC");
    tracker.recordContextCreation(config2, 300);
    tracker.recordTestClassForContext(config2, "com.example.TestD");
    tracker.recordContextCacheHit(config2);

    assertEquals(2, tracker.getCacheHits());
    assertEquals(2, tracker.getCacheMisses());
    assertEquals(2, tracker.getTotalContextsCreated());
    assertEquals(50.0, tracker.getContextReuseRate(), 0.01);
  }

  @Test
  void shouldResetCountersOnClear() {
    MergedContextConfiguration config = createConfig(Object.class);

    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);
    tracker.recordTestClassForContext(config, "com.example.TestB");
    tracker.recordContextCacheHit(config);

    tracker.clear();

    assertEquals(0, tracker.getCacheHits());
    assertEquals(0, tracker.getCacheMisses());
    assertEquals(0, tracker.getTotalContextsCreated());
    assertEquals(0.0, tracker.getContextReuseRate());
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
