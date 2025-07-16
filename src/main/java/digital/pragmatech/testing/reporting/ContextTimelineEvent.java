package digital.pragmatech.testing.reporting;

/**
 * Timeline event for Chart.js visualization showing context creation timing.
 */
public record ContextTimelineEvent(
  String contextName,
  String color,
  long creationTimeSeconds,
  long loadTimeMs,
  int testClassCount,
  int cacheHitCount,
  int beanCount) {
}
