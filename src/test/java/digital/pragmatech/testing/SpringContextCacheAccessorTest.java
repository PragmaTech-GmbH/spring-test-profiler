package digital.pragmatech.testing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringContextCacheAccessorTest {

  @Test
  void shouldRetrieveMaxCacheSize() {
    // When
    int maxCacheSize = SpringContextCacheAccessor.getMaxCacheSize();

    // Then
    assertTrue(maxCacheSize > 0, "Max cache size should be positive");
    // Default Spring cache size is 32
    assertTrue(maxCacheSize >= 32, "Max cache size should be at least 32 (default)");
  }

  @Test
  void shouldCreateCacheStatisticsWithMaxSize() {
    // When
    SpringContextCacheAccessor.CacheStatistics stats = SpringContextCacheAccessor.getCacheStatistics(null);

    // Then
    assertNotNull(stats);
    assertTrue(stats.getMaxSize() > 0, "Max size should be positive");
    assertEquals(0, stats.getSize());
    assertEquals(0, stats.getHitCount());
    assertEquals(0, stats.getMissCount());
  }

  @Test
  void shouldIncludeMaxSizeInToString() {
    // When
    SpringContextCacheAccessor.CacheStatistics stats = SpringContextCacheAccessor.getCacheStatistics(null);
    String toString = stats.toString();

    // Then
    assertTrue(toString.contains("maxSize="), "toString should include maxSize");
    assertTrue(toString.contains("size=0"), "toString should include size");
    assertTrue(toString.contains("hitCount=0"), "toString should include hitCount");
  }
}
