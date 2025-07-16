package digital.pragmatech.testing;

import java.time.Instant;
import java.util.List;

import digital.pragmatech.testing.reporting.ContextTimelineEvent;

/**
 * Data structure for timeline visualization.
 */
public record TimelineData(List<TimelineEntry> entries, Instant startTime, Instant endTime, List<ContextTimelineEvent> events) {
}
