package digital.pragmatech.testing;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void shouldRecordRemovalOnLastLifespan() {
    MergedContextConfiguration config = createConfig(Object.class);
    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);

    Instant removalTime = Instant.now();
    tracker.recordContextRemoval(config, removalTime, ContextRemovalReason.DIRTIES_CONTEXT);

    ContextCacheEntry entry = tracker.getCacheEntry(config).orElseThrow();
    assertTrue(entry.isCurrentlyRemoved());
    assertEquals(1, entry.getLifespans().size());

    ContextLifespan lifespan = entry.getLifespans().get(0);
    assertTrue(lifespan.isRemoved());
    assertEquals(removalTime, lifespan.getRemovalTime());
    assertEquals(ContextRemovalReason.DIRTIES_CONTEXT, lifespan.getRemovalReason());
  }

  @Test
  void shouldTrackRecreationAfterRemovalAsNewLifespanAndMiss() {
    MergedContextConfiguration config = createConfig(Object.class);
    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);
    tracker.recordContextRemoval(config, Instant.now(), ContextRemovalReason.DIRTIES_CONTEXT);

    // Same configuration is loaded again after removal
    tracker.recordContextCreation(config, 300);

    ContextCacheEntry entry = tracker.getCacheEntry(config).orElseThrow();
    assertEquals(2, entry.getLifespans().size());
    assertTrue(entry.getLifespans().get(0).isRemoved());
    assertFalse(entry.getLifespans().get(1).isRemoved());
    assertFalse(entry.isCurrentlyRemoved());

    assertEquals(2, tracker.getCacheMisses());
    assertEquals(2, tracker.getTotalContextsCreated());
  }

  @Test
  void shouldIgnoreRemovalForUnknownConfig() {
    MergedContextConfiguration config = createConfig(Object.class);

    assertDoesNotThrow(
        () -> tracker.recordContextRemoval(config, Instant.now(), ContextRemovalReason.UNKNOWN));
  }

  @Test
  void shouldIgnoreRemovalWhenNoLifespanIsOpen() {
    MergedContextConfiguration config = createConfig(Object.class);
    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);

    tracker.recordContextRemoval(config, Instant.now(), ContextRemovalReason.DIRTIES_CONTEXT);
    // Second removal without a re-creation in between must not change anything
    tracker.recordContextRemoval(config, Instant.now(), ContextRemovalReason.CACHE_EVICTION);

    ContextCacheEntry entry = tracker.getCacheEntry(config).orElseThrow();
    assertEquals(1, entry.getLifespans().size());
    assertEquals(
        ContextRemovalReason.DIRTIES_CONTEXT, entry.getLifespans().get(0).getRemovalReason());
  }

  @Test
  void shouldNotBeCurrentlyRemovedWhileLifespanIsOpen() {
    MergedContextConfiguration config = createConfig(Object.class);
    tracker.recordTestClassForContext(config, "com.example.TestA");
    tracker.recordContextCreation(config, 500);

    ContextCacheEntry entry = tracker.getCacheEntry(config).orElseThrow();
    assertFalse(entry.isCurrentlyRemoved());
    assertNull(entry.getLifespans().get(0).getRemovalTime());
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
