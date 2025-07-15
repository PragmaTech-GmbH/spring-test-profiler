package digital.pragmatech.testing;

import java.time.Instant;
import java.util.List;

/**
 * Data structure for timeline visualization.
 */
public record TimelineData(List<TimelineEntry> entries, Instant startTime, Instant endTime, List<ContextTimelineEvent> events) {

  public long getTotalDurationMs() {
    return startTime != null && endTime != null ?
      java.time.Duration.between(startTime, endTime).toMillis() : 0;
  }

  public long getTotalDurationSeconds() {
    return getTotalDurationMs() / 1000;
  }
}
