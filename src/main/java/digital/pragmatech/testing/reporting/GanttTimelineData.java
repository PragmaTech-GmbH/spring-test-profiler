package digital.pragmatech.testing.reporting;

import java.util.List;

/** Root data structure for the Gantt-style timeline visualization. */
public record GanttTimelineData(long totalDurationMs, List<ContextLane> contextLanes) {

  /** Returns the total number of test methods across all contexts. */
  public int totalTestMethods() {
    if (contextLanes == null) {
      return 0;
    }
    return contextLanes.stream().mapToInt(ContextLane::testMethodCount).sum();
  }
}
