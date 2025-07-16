package digital.pragmatech.testing.reporting;

import java.util.Map;

import digital.pragmatech.testing.SpringContextStatistics;
import digital.pragmatech.testing.TestStatus;

public record TestClassExecutionData(String className, Map<String, TestExecutionData> testExecutions, SpringContextStatistics contextStatistics) {

  public long getTotalTests() {
    return testExecutions.size();
  }

  public long getPassedTests() {
    return testExecutions.values().stream()
      .filter(data -> data.getStatus() == TestStatus.PASSED)
      .count();
  }

  public long getFailedTests() {
    return testExecutions.values().stream()
      .filter(data -> data.getStatus() == TestStatus.FAILED)
      .count();
  }

  public long getDisabledTests() {
    return testExecutions.values().stream()
      .filter(data -> data.getStatus() == TestStatus.DISABLED)
      .count();
  }

  public long getAbortedTests() {
    return testExecutions.values().stream()
      .filter(data -> data.getStatus() == TestStatus.ABORTED)
      .count();
  }
}
