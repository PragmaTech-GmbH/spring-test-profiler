package digital.pragmatech.testing;

import java.time.Instant;

/**
 * One lifetime of an application context in Spring's test context cache: from creation until
 * removal (via @DirtiesContext or LRU eviction) or until the end of the test run. A single {@link
 * ContextCacheEntry} can accumulate multiple lifespans when the same MergedContextConfiguration is
 * re-created after removal (e.g. @DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)).
 */
public class ContextLifespan {

  private final Instant creationTime;
  private final long loadTimeMs;
  private volatile Instant removalTime;
  private volatile ContextRemovalReason removalReason;

  public ContextLifespan(Instant creationTime, long loadTimeMs) {
    this.creationTime = creationTime;
    this.loadTimeMs = loadTimeMs;
  }

  /** Marks this lifespan as removed. No-op if a removal was already recorded. */
  public void markRemoved(Instant removalTime, ContextRemovalReason removalReason) {
    if (this.removalTime == null) {
      this.removalTime = removalTime;
      this.removalReason = removalReason;
    }
  }

  public boolean isRemoved() {
    return removalTime != null;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public long getLoadTimeMs() {
    return loadTimeMs;
  }

  /** The time this context was removed from the cache, or null if still cached. */
  public Instant getRemovalTime() {
    return removalTime;
  }

  /** The reason for removal, or null if still cached. */
  public ContextRemovalReason getRemovalReason() {
    return removalReason;
  }
}
