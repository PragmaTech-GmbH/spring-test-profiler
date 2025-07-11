package digital.pragmatech.springtestinsight;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks test execution metrics across all test classes and methods.
 */
public class TestExecutionTracker {
    
    private final Map<String, TestClassMetrics> classMetrics = new ConcurrentHashMap<>();
    private final AtomicInteger totalTestClasses = new AtomicInteger(0);
    private final AtomicInteger totalTestMethods = new AtomicInteger(0);
    private Instant overallStartTime;
    private Instant overallEndTime;
    
    public void startTracking() {
        overallStartTime = Instant.now();
    }
    
    public void stopTracking() {
        overallEndTime = Instant.now();
    }
    
    public void recordTestClassStart(String className) {
        classMetrics.computeIfAbsent(className, k -> {
            totalTestClasses.incrementAndGet();
            return new TestClassMetrics(className);
        }).recordStart();
    }
    
    public void recordTestClassEnd(String className) {
        TestClassMetrics metrics = classMetrics.get(className);
        if (metrics != null) {
            metrics.recordEnd();
        }
    }
    
    public void recordTestMethodStart(String className, String methodName) {
        TestClassMetrics metrics = classMetrics.get(className);
        if (metrics != null) {
            metrics.recordMethodStart(methodName);
            totalTestMethods.incrementAndGet();
        }
    }
    
    public void recordTestMethodEnd(String className, String methodName, TestStatus status) {
        TestClassMetrics metrics = classMetrics.get(className);
        if (metrics != null) {
            metrics.recordMethodEnd(methodName, status);
        }
    }
    
    public Map<String, TestClassMetrics> getClassMetrics() {
        return Collections.unmodifiableMap(classMetrics);
    }
    
    public int getTotalTestClasses() {
        return totalTestClasses.get();
    }
    
    public int getTotalTestMethods() {
        return totalTestMethods.get();
    }
    
    public Duration getOverallDuration() {
        if (overallStartTime != null && overallEndTime != null) {
            return Duration.between(overallStartTime, overallEndTime);
        }
        return Duration.ZERO;
    }
    
    /**
     * Metrics for a single test class.
     */
    public static class TestClassMetrics {
        private final String className;
        private final Map<String, TestMethodMetrics> methodMetrics = new ConcurrentHashMap<>();
        private Instant startTime;
        private Instant endTime;
        
        public TestClassMetrics(String className) {
            this.className = className;
        }
        
        public void recordStart() {
            this.startTime = Instant.now();
        }
        
        public void recordEnd() {
            this.endTime = Instant.now();
        }
        
        public void recordMethodStart(String methodName) {
            methodMetrics.computeIfAbsent(methodName, TestMethodMetrics::new).recordStart();
        }
        
        public void recordMethodEnd(String methodName, TestStatus status) {
            TestMethodMetrics metrics = methodMetrics.get(methodName);
            if (metrics != null) {
                metrics.recordEnd(status);
            }
        }
        
        public String getClassName() {
            return className;
        }
        
        public Duration getDuration() {
            if (startTime != null && endTime != null) {
                return Duration.between(startTime, endTime);
            }
            return Duration.ZERO;
        }
        
        public Map<String, TestMethodMetrics> getMethodMetrics() {
            return Collections.unmodifiableMap(methodMetrics);
        }
        
        public int getTotalMethods() {
            return methodMetrics.size();
        }
        
        public long getPassedMethods() {
            return methodMetrics.values().stream()
                .filter(m -> m.getStatus() == TestStatus.PASSED)
                .count();
        }
        
        public long getFailedMethods() {
            return methodMetrics.values().stream()
                .filter(m -> m.getStatus() == TestStatus.FAILED)
                .count();
        }
    }
    
    /**
     * Metrics for a single test method.
     */
    public static class TestMethodMetrics {
        private final String methodName;
        private Instant startTime;
        private Instant endTime;
        private TestStatus status;
        
        public TestMethodMetrics(String methodName) {
            this.methodName = methodName;
        }
        
        public void recordStart() {
            this.startTime = Instant.now();
        }
        
        public void recordEnd(TestStatus status) {
            this.endTime = Instant.now();
            this.status = status;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public Duration getDuration() {
            if (startTime != null && endTime != null) {
                return Duration.between(startTime, endTime);
            }
            return Duration.ZERO;
        }
        
        public TestStatus getStatus() {
            return status;
        }
    }
}