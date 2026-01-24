package digital.pragmatech.testing.reporting;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.ContextIdGenerator;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestExecutionTracker.TestClassMetrics;
import digital.pragmatech.testing.TestExecutionTracker.TestMethodMetrics;
import digital.pragmatech.testing.TestStatus;

/**
 * Generates data for Gantt-style timeline visualization showing context loading and test execution
 * timing.
 */
public class GanttTimelineDataGenerator {

  private static final List<String> CONTEXT_COLORS =
      Arrays.asList(
          "#e74c3c", "#3498db", "#27ae60", "#f39c12", "#9b59b6", "#e67e22", "#1abc9c", "#34495e",
          "#e91e63", "#ff5722");

  /**
   * Generates Gantt timeline data from context cache and test execution trackers.
   *
   * @param contextTracker the context cache tracker with context lifecycle data
   * @param executionTracker the test execution tracker with test timing data
   * @return the Gantt timeline data for visualization
   */
  public GanttTimelineData generate(
      ContextCacheTracker contextTracker, TestExecutionTracker executionTracker) {

    if (contextTracker == null) {
      return new GanttTimelineData(0, List.of());
    }

    // Get all created context entries sorted by creation time
    List<ContextCacheEntry> createdEntries =
        contextTracker.getAllEntries().stream()
            .filter(ContextCacheEntry::isCreated)
            .filter(entry -> entry.getCreationTime() != null)
            .sorted(Comparator.comparing(ContextCacheEntry::getCreationTime))
            .toList();

    if (createdEntries.isEmpty()) {
      return new GanttTimelineData(0, List.of());
    }

    // Find reference time (T=0) - earliest of context creation or overall test start
    Instant referenceTime = findReferenceTime(createdEntries, executionTracker);

    // Build context lanes with test execution markers
    List<ContextLane> contextLanes = new ArrayList<>();
    Map<String, TestClassMetrics> classMetrics =
        executionTracker != null ? executionTracker.getClassMetrics() : Map.of();

    for (int i = 0; i < createdEntries.size(); i++) {
      ContextCacheEntry entry = createdEntries.get(i);
      ContextLane lane = buildContextLane(entry, i, referenceTime, classMetrics);
      contextLanes.add(lane);
    }

    // Calculate total duration
    long totalDurationMs = calculateTotalDuration(contextLanes, classMetrics, referenceTime);

    return new GanttTimelineData(totalDurationMs, contextLanes);
  }

  private Instant findReferenceTime(
      List<ContextCacheEntry> entries, TestExecutionTracker executionTracker) {
    Instant earliest = entries.get(0).getCreationTime();

    if (executionTracker != null && executionTracker.getOverallStartTime() != null) {
      Instant overallStart = executionTracker.getOverallStartTime();
      if (overallStart.isBefore(earliest)) {
        earliest = overallStart;
      }
    }

    return earliest;
  }

  private ContextLane buildContextLane(
      ContextCacheEntry entry,
      int index,
      Instant referenceTime,
      Map<String, TestClassMetrics> classMetrics) {

    String contextId = ContextIdGenerator.getContextId(entry.getConfiguration());
    String color = CONTEXT_COLORS.get(index % CONTEXT_COLORS.size());

    // Calculate context load timing relative to reference
    long loadStartMs = Duration.between(referenceTime, entry.getCreationTime()).toMillis();
    long loadDurationMs = entry.getContextLoadTimeMs();
    long loadEndMs = loadStartMs + loadDurationMs;

    // Generate context label using context ID
    String contextLabel = generateContextLabel(entry, index, contextId);

    // Build test execution markers for tests using this context
    List<TestExecutionMarker> testExecutions =
        buildTestExecutionMarkers(entry, referenceTime, classMetrics);

    return new ContextLane(
        contextId,
        contextLabel,
        color,
        loadStartMs,
        loadEndMs,
        loadDurationMs,
        entry.getBeanDefinitionCount(),
        entry.getTestClasses().size(),
        entry.getTestMethods().size(),
        testExecutions);
  }

  private String generateContextLabel(ContextCacheEntry entry, int index, String contextId) {
    // Use the context ID as the label for the Y-axis
    return contextId;
  }

  private List<TestExecutionMarker> buildTestExecutionMarkers(
      ContextCacheEntry entry, Instant referenceTime, Map<String, TestClassMetrics> classMetrics) {

    List<TestExecutionMarker> markers = new ArrayList<>();

    // Iterate through test methods that used this context
    for (String testMethodId : entry.getTestMethods()) {
      // Format is "ClassName#methodName"
      String[] parts = testMethodId.split("#");
      if (parts.length != 2) {
        continue;
      }

      String testClassName = parts[0];
      String methodName = parts[1];

      // Find the test metrics
      TestClassMetrics classMetric = classMetrics.get(testClassName);
      if (classMetric == null) {
        continue;
      }

      TestMethodMetrics methodMetric = classMetric.getMethodMetrics().get(methodName);
      if (methodMetric == null || methodMetric.getStartTime() == null) {
        continue;
      }

      // Calculate timing relative to reference
      long startMs = Duration.between(referenceTime, methodMetric.getStartTime()).toMillis();
      long endMs =
          methodMetric.getEndTime() != null
              ? Duration.between(referenceTime, methodMetric.getEndTime()).toMillis()
              : startMs + 1; // Minimum 1ms duration if end time not recorded
      long durationMs = endMs - startMs;

      // Get status and color
      TestStatus status = methodMetric.getStatus();
      String statusName = status != null ? status.name() : "UNKNOWN";
      String statusColor = getStatusColor(status);

      // Generate display name (simple class name + method)
      String simpleClassName = testClassName.substring(testClassName.lastIndexOf('.') + 1);
      String displayName = simpleClassName + "." + methodName;

      markers.add(
          new TestExecutionMarker(
              testClassName,
              methodName,
              displayName,
              startMs,
              endMs,
              durationMs,
              statusName,
              statusColor));
    }

    // Sort by start time
    markers.sort(Comparator.comparingLong(TestExecutionMarker::startMs));

    return markers;
  }

  private String getStatusColor(TestStatus status) {
    if (status == null) {
      return "#95a5a6"; // Gray for unknown
    }
    return switch (status) {
      case PASSED -> "#27ae60"; // Green
      case FAILED -> "#e74c3c"; // Red
      case ABORTED -> "#f39c12"; // Orange
      case DISABLED -> "#95a5a6"; // Gray
      default -> "#3498db"; // Blue for running/pending
    };
  }

  private long calculateTotalDuration(
      List<ContextLane> lanes, Map<String, TestClassMetrics> classMetrics, Instant referenceTime) {

    long maxEndTime = 0;

    // Check context load end times
    for (ContextLane lane : lanes) {
      maxEndTime = Math.max(maxEndTime, lane.loadEndMs());
      // Check test execution end times
      for (TestExecutionMarker marker : lane.testExecutions()) {
        maxEndTime = Math.max(maxEndTime, marker.endMs());
      }
    }

    // Also check all test class end times
    for (TestClassMetrics classMetric : classMetrics.values()) {
      if (classMetric.getEndTime() != null) {
        long endMs = Duration.between(referenceTime, classMetric.getEndTime()).toMillis();
        maxEndTime = Math.max(maxEndTime, endMs);
      }
    }

    return maxEndTime;
  }
}
