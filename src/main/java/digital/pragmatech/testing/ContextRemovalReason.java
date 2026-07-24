package digital.pragmatech.testing;

/** Reason why an application context was removed from Spring's test context cache. */
public enum ContextRemovalReason {
  DIRTIES_CONTEXT,
  CACHE_EVICTION,
  UNKNOWN
}
