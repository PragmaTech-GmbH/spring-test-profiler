package digital.pragmatech.testing.reporting;

import java.util.List;

/** Represents a single context lane in the Gantt timeline visualization. */
public record ContextLane(
    String contextId,
    String contextLabel,
    String color,
    long loadStartMs,
    long loadEndMs,
    long loadDurationMs,
    int beanCount,
    int testClassCount,
    int testMethodCount,
    List<TestExecutionMarker> testExecutions) {}
