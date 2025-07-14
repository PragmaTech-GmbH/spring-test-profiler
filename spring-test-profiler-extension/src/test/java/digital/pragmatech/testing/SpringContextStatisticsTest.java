package digital.pragmatech.testing;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringContextStatisticsTest {

  private SpringContextStatistics statistics;

  @BeforeEach
  void setUp() {
    statistics = new SpringContextStatistics();
  }

  @Test
  void testInitialState() {
    assertEquals(0, statistics.getContextLoads());
    assertEquals(0, statistics.getCacheHits());
    assertEquals(0, statistics.getCacheMisses());
    assertEquals(0.0, statistics.getCacheHitRate());
    assertTrue(statistics.getContextLoadEvents().isEmpty());
    assertEquals(Duration.ZERO, statistics.getTotalContextLoadTime());
  }

  @Test
  void testRecordContextLoad() {
    statistics.recordContextLoad("test-context-1", Duration.ofMillis(500));

    assertEquals(1, statistics.getContextLoads());
    assertEquals(0, statistics.getCacheHits());
    assertEquals(1, statistics.getCacheMisses());
    assertEquals(1, statistics.getContextLoadEvents().size());

    var event = statistics.getContextLoadEvents().get(0);
    assertEquals("test-context-1", event.getContextKey());
    assertEquals(Duration.ofMillis(500), event.getLoadTime());
    assertNotNull(event.getTimestamp());
  }

  @Test
  void testRecordCacheHit() {
    statistics.recordCacheHit("test-context-1");

    assertEquals(0, statistics.getContextLoads());
    assertEquals(1, statistics.getCacheHits());
    assertEquals(0, statistics.getCacheMisses());
  }

  @Test
  void testCacheHitRate() {
    // Record some cache misses
    statistics.recordContextLoad("context-1", Duration.ofMillis(100));
    statistics.recordContextLoad("context-2", Duration.ofMillis(200));

    // Record some cache hits
    statistics.recordCacheHit("context-1");
    statistics.recordCacheHit("context-1");
    statistics.recordCacheHit("context-2");

    // 3 hits out of 5 total accesses = 60%
    assertEquals(60.0, statistics.getCacheHitRate(), 0.01);
  }

  @Test
  void testCacheHitRateWithNoAccesses() {
    assertEquals(0.0, statistics.getCacheHitRate());
  }

  @Test
  void testTotalContextLoadTime() {
    statistics.recordContextLoad("context-1", Duration.ofMillis(500));
    statistics.recordContextLoad("context-2", Duration.ofMillis(300));
    statistics.recordContextLoad("context-3", Duration.ofMillis(200));

    assertEquals(Duration.ofMillis(1000), statistics.getTotalContextLoadTime());
  }

  @Test
  void testMultipleContextLoadsAndHits() {
    // Simulate a realistic scenario
    statistics.recordContextLoad("test-context", Duration.ofMillis(1000));
    statistics.recordCacheHit("test-context");
    statistics.recordCacheHit("test-context");
    statistics.recordContextLoad("integration-context", Duration.ofMillis(2000));
    statistics.recordCacheHit("integration-context");

    assertEquals(2, statistics.getContextLoads());
    assertEquals(3, statistics.getCacheHits());
    assertEquals(2, statistics.getCacheMisses());
    assertEquals(60.0, statistics.getCacheHitRate(), 0.01); // 3 hits out of 5 total
    assertEquals(Duration.ofMillis(3000), statistics.getTotalContextLoadTime());
  }
}
