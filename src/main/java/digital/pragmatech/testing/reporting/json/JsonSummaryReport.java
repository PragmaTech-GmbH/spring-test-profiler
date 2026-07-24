package digital.pragmatech.testing.reporting.json;

/**
 * Flat, machine-readable summary of a single profiler run. Written as results.json (latest run) and
 * as a timestamped sibling of the HTML report so CI pipelines can consume the metrics with simple
 * tooling like jq.
 *
 * <p>All duration values are in milliseconds. The schemaVersion field allows evolving this format
 * without breaking consumers.
 *
 * <p>Test-level counts (classes, methods, pass/fail) are intentionally not included - build tools
 * like Surefire, Failsafe, and Gradle already report them.
 */
public record JsonSummaryReport(
    int schemaVersion,
    String profilerVersion,
    String generatedAt,
    String buildTool,
    long totalDurationMs,
    int contextsCreated,
    int contextCacheHits,
    int contextCacheMisses,
    double contextCacheHitRatio,
    int springContextCacheSize,
    int springContextCacheMaxSize,
    long totalContextCreationTimeMs,
    long wastedContextTimeMs,
    long potentialTimeSavingsMs,
    double potentialTimeSavingsPercent,
    Integer availableProcessors) {

  public static final int CURRENT_SCHEMA_VERSION = 1;
}
