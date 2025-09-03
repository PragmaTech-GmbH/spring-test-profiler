package digital.pragmatech.testing.optimization;

public record OptimizationRecord(
    String testClass,
    String testSmellType,
    String title,
    String description,
    String recommendation,
    Severity severity,
    String sourceLocation) {
  public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}
