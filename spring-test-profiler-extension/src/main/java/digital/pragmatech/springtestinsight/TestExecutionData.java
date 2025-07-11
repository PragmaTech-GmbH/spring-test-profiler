package digital.pragmatech.springtestinsight;

import java.time.Duration;
import java.time.Instant;

public class TestExecutionData {
    private final String testId;
    private final Instant startTime;
    private volatile Instant endTime;
    private volatile TestStatus status = TestStatus.RUNNING;
    private volatile Throwable throwable;
    private volatile String reason;
    
    public TestExecutionData(String testId, Instant startTime) {
        this.testId = testId;
        this.startTime = startTime;
    }
    
    public String getTestId() {
        return testId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public synchronized void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public Duration getDuration() {
        if (endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return null;
    }
    
    public TestStatus getStatus() {
        return status;
    }
    
    public synchronized void setStatus(TestStatus status) {
        this.status = status;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
    public synchronized void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
    
    public String getReason() {
        return reason;
    }
    
    public synchronized void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getTestMethodName() {
        return testId.substring(testId.lastIndexOf('#') + 1);
    }
    
    public String getTestClassName() {
        return testId.substring(0, testId.lastIndexOf('#'));
    }
}