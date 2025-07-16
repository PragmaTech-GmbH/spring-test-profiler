package digital.pragmatech.testing;

import java.util.List;

import digital.pragmatech.testing.optimization.ContextOptimizationOpportunity;

/**
 * Statistics about potential optimizations.
 */
public class OptimizationStatistics {
  private final long totalContextCreationTimeMs;
  private final long potentialTimeSavingsMs;
  private final long wastedTimeMs;
  private final int totalContextsCreated;
  private final List<ContextOptimizationOpportunity> topOpportunities;

  public OptimizationStatistics(long totalContextCreationTimeMs, long potentialTimeSavingsMs,
    long wastedTimeMs, int totalContextsCreated,
    List<ContextOptimizationOpportunity> topOpportunities) {
    this.totalContextCreationTimeMs = totalContextCreationTimeMs;
    this.potentialTimeSavingsMs = potentialTimeSavingsMs;
    this.wastedTimeMs = wastedTimeMs;
    this.totalContextsCreated = totalContextsCreated;
    this.topOpportunities = topOpportunities;
  }

  public long getTotalContextCreationTimeMs() {
    return totalContextCreationTimeMs;
  }

  public long getPotentialTimeSavingsMs() {
    return potentialTimeSavingsMs;
  }

  public long getWastedTimeMs() {
    return wastedTimeMs;
  }

  public int getTotalContextsCreated() {
    return totalContextsCreated;
  }

  public List<ContextOptimizationOpportunity> getTopOpportunities() {
    return topOpportunities;
  }

  public double getPotentialTimeSavingsPercentage() {
    return totalContextCreationTimeMs > 0 ?
      (potentialTimeSavingsMs * 100.0) / totalContextCreationTimeMs : 0.0;
  }
}
