package digital.pragmatech.testing;

public record ContextOptimizationOpportunity(
  String testClass,
  long loadTimeMs,
  int beanCount,
  String recommendation) {
}
