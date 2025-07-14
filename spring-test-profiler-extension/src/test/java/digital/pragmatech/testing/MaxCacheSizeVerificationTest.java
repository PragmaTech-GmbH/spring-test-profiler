package digital.pragmatech.testing;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.cache.ContextCacheUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to verify that ContextCacheUtils.retrieveMaxCacheSize() works as expected.
 */
class MaxCacheSizeVerificationTest {

  @Test
  void shouldRetrieveMaxCacheSizeDirectly() {
    // When
    int maxCacheSize = ContextCacheUtils.retrieveMaxCacheSize();

    // Then
    System.out.println("Max cache size from ContextCacheUtils: " + maxCacheSize);
    assertTrue(maxCacheSize > 0, "Max cache size should be positive");
    assertEquals(32, maxCacheSize, "Default max cache size should be 32");
  }

  @Test
  void shouldAccessorReturnSameValue() {
    // When
    int directValue = ContextCacheUtils.retrieveMaxCacheSize();
    int accessorValue = SpringContextCacheAccessor.getMaxCacheSize();

    // Then
    assertEquals(directValue, accessorValue, "Accessor should return same value as direct call");
  }
}
