package digital.pragmatech.testing;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a context removal triggered by @DirtiesContext is recorded as a closed lifespan
 * with the correct removal reason, and that the subsequent re-creation opens a new lifespan.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DirtiesContextRemovalTrackingTest.RemovalTrackingConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DirtiesContextRemovalTrackingTest {

  @Test
  @Order(1)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void dirtiesTheContext() {
    // The context is removed from the cache after this method
  }

  @Test
  @Order(2)
  void shouldHaveRecordedRemovalWithDirtiesContextReason() {
    ContextCacheTracker tracker = SpringTestProfilerListener.getContextCacheTracker();
    Optional<MergedContextConfiguration> config =
        tracker.getContextForTestClass(getClass().getName());
    assertTrue(config.isPresent(), "Context configuration should be tracked for this test class");

    ContextCacheEntry entry = tracker.getCacheEntry(config.get()).orElseThrow();
    List<ContextLifespan> lifespans = entry.getLifespans();
    assertEquals(2, lifespans.size(), "Removal + re-creation should produce two lifespans");

    ContextLifespan removedLifespan = lifespans.get(0);
    assertTrue(removedLifespan.isRemoved(), "First lifespan should be removed by @DirtiesContext");
    assertNotNull(removedLifespan.getRemovalTime());
    assertEquals(ContextRemovalReason.DIRTIES_CONTEXT, removedLifespan.getRemovalReason());

    ContextLifespan currentLifespan = lifespans.get(1);
    assertFalse(currentLifespan.isRemoved(), "Re-created context should still be cached");
    assertFalse(entry.isCurrentlyRemoved());
  }

  @Configuration
  static class RemovalTrackingConfig {}
}
