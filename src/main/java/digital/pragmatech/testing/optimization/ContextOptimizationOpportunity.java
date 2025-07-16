package digital.pragmatech.testing.optimization;

public record ContextOptimizationOpportunity(
  String testClass,
  long loadTimeMs,
  int beanCount,
  String recommendation) {
}
