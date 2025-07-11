package digital.pragmatech.springtestinsight;

import java.util.Map;

public class TestClassExecutionData {
    private final String className;
    private final Map<String, TestExecutionData> testExecutions;
    private final SpringContextStatistics contextStatistics;
    
    public TestClassExecutionData(String className, 
                                  Map<String, TestExecutionData> testExecutions,
                                  SpringContextStatistics contextStatistics) {
        this.className = className;
        this.testExecutions = testExecutions;
        this.contextStatistics = contextStatistics;
    }
    
    public String getClassName() {
        return className;
    }
    
    public Map<String, TestExecutionData> getTestExecutions() {
        return testExecutions;
    }
    
    public SpringContextStatistics getContextStatistics() {
        return contextStatistics;
    }
    
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