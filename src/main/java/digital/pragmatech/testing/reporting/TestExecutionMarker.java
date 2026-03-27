package digital.pragmatech.testing.reporting;

/** Represents a single test execution marker on the Gantt timeline. */
public record TestExecutionMarker(
    String testClassName,
    String testMethodName,
    String displayName,
    long startMs,
    long endMs,
    long durationMs,
    String status,
    String statusColor) {}
