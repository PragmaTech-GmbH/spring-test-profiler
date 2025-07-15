package digital.pragmatech.testing;

/**
 * Individual timeline entry for visualization.
 */
public record TimelineEntry(
  String contextLabel,
  String phase,
  long startMs,
  long endMs,
  String color,
  String tooltip,
  int contextId) {

  public long getDurationMs() {
    return endMs - startMs;
  }
}
