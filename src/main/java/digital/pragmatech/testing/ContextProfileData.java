package digital.pragmatech.testing;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive profile data for a Spring application context.
 * Tracks timing, memory usage, bean creation metrics, and lifecycle phases.
 */
public class ContextProfileData {
    
    private final String contextId;
    private final Instant startTime;
    private final long startMemoryBytes;
    
    // Completion data
    private volatile Instant endTime;
    private volatile long endMemoryBytes;
    private volatile long totalLoadTimeMs;
    private volatile long memoryUsedMB;
    
    // Bean metrics
    private volatile int beanDefinitionCount;
    private volatile BeanCreationProfiler.BeanCreationMetrics beanCreationMetrics;
    
    // Lifecycle phases (e.g., BeanDefinitionRegistration, ContextRefreshedEvent)
    private final Map<String, Instant> lifecyclePhases = new ConcurrentHashMap<>();
    
    // Additional context information
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    public ContextProfileData(String contextId, Instant startTime, long startMemoryBytes) {
        this.contextId = contextId;
        this.startTime = startTime;
        this.startMemoryBytes = startMemoryBytes;
    }
    
    // Lifecycle phase tracking
    public void recordPhase(String phaseName, Instant timestamp) {
        lifecyclePhases.put(phaseName, timestamp);
    }
    
    public Instant getPhaseTime(String phaseName) {
        return lifecyclePhases.get(phaseName);
    }
    
    public Map<String, Instant> getAllPhases() {
        return new ConcurrentHashMap<>(lifecyclePhases);
    }
    
    // Duration calculations
    public long getPhaseElapsedMs(String phaseName) {
        Instant phaseTime = lifecyclePhases.get(phaseName);
        if (phaseTime != null) {
            return java.time.Duration.between(startTime, phaseTime).toMillis();
        }
        return -1;
    }
    
    public long getBeanDefinitionRegistrationTimeMs() {
        return getPhaseElapsedMs("BeanDefinitionRegistration");
    }
    
    public long getContextRefreshTimeMs() {
        return getPhaseElapsedMs("ContextRefreshedEvent");
    }
    
    // Metadata operations
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    // Bean creation analysis
    public List<BeanCreationProfiler.BeanCreationMetric> getSlowestBeans(int limit) {
        if (beanCreationMetrics != null) {
            return beanCreationMetrics.getAllBeans().stream()
                .sorted((a, b) -> Long.compare(b.getCreationTimeMs(), a.getCreationTimeMs()))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return new ArrayList<>();
    }
    
    public double getBeanCreationEfficiency() {
        if (beanCreationMetrics != null && totalLoadTimeMs > 0) {
            return (double) beanCreationMetrics.getTotalCreationTimeMs() / totalLoadTimeMs;
        }
        return 0.0;
    }
    
    // Memory analysis
    public double getMemoryEfficiencyMBPerBean() {
        if (beanCreationMetrics != null && beanCreationMetrics.getTotalBeansCreated() > 0) {
            return (double) memoryUsedMB / beanCreationMetrics.getTotalBeansCreated();
        }
        return 0.0;
    }
    
    // Summary methods
    public ContextProfileSummary getSummary() {
        return new ContextProfileSummary(
            contextId,
            totalLoadTimeMs,
            memoryUsedMB,
            beanDefinitionCount,
            beanCreationMetrics != null ? beanCreationMetrics.getTotalBeansCreated() : 0,
            beanCreationMetrics != null ? beanCreationMetrics.getSlowestBeanTimeMs() : 0,
            beanCreationMetrics != null ? beanCreationMetrics.getSlowestBeanName() : null,
            getBeanCreationEfficiency(),
            getMemoryEfficiencyMBPerBean()
        );
    }
    
    // Getters and setters
    public String getContextId() { return contextId; }
    public Instant getStartTime() { return startTime; }
    public long getStartMemoryBytes() { return startMemoryBytes; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public long getEndMemoryBytes() { return endMemoryBytes; }
    public void setEndMemory(long endMemoryBytes) { this.endMemoryBytes = endMemoryBytes; }
    
    public long getTotalLoadTimeMs() { return totalLoadTimeMs; }
    public void setTotalLoadTimeMs(long totalLoadTimeMs) { this.totalLoadTimeMs = totalLoadTimeMs; }
    
    public long getMemoryUsedMB() { return memoryUsedMB; }
    public void setMemoryUsedMB(long memoryUsedMB) { this.memoryUsedMB = memoryUsedMB; }
    
    public int getBeanDefinitionCount() { return beanDefinitionCount; }
    public void setBeanDefinitionCount(int beanDefinitionCount) { this.beanDefinitionCount = beanDefinitionCount; }
    
    public BeanCreationProfiler.BeanCreationMetrics getBeanCreationMetrics() { return beanCreationMetrics; }
    public void setBeanCreationMetrics(BeanCreationProfiler.BeanCreationMetrics beanCreationMetrics) { 
        this.beanCreationMetrics = beanCreationMetrics; 
    }
    
    /**
     * Summary data for quick reporting.
     */
    public static class ContextProfileSummary {
        private final String contextId;
        private final long totalLoadTimeMs;
        private final long memoryUsedMB;
        private final int beanDefinitionCount;
        private final long beansCreated;
        private final long slowestBeanTimeMs;
        private final String slowestBeanName;
        private final double beanCreationEfficiency;
        private final double memoryEfficiencyMBPerBean;
        
        public ContextProfileSummary(String contextId, long totalLoadTimeMs, long memoryUsedMB,
                                   int beanDefinitionCount, long beansCreated, long slowestBeanTimeMs,
                                   String slowestBeanName, double beanCreationEfficiency, double memoryEfficiencyMBPerBean) {
            this.contextId = contextId;
            this.totalLoadTimeMs = totalLoadTimeMs;
            this.memoryUsedMB = memoryUsedMB;
            this.beanDefinitionCount = beanDefinitionCount;
            this.beansCreated = beansCreated;
            this.slowestBeanTimeMs = slowestBeanTimeMs;
            this.slowestBeanName = slowestBeanName;
            this.beanCreationEfficiency = beanCreationEfficiency;
            this.memoryEfficiencyMBPerBean = memoryEfficiencyMBPerBean;
        }
        
        // Getters
        public String getContextId() { return contextId; }
        public long getTotalLoadTimeMs() { return totalLoadTimeMs; }
        public long getMemoryUsedMB() { return memoryUsedMB; }
        public int getBeanDefinitionCount() { return beanDefinitionCount; }
        public long getBeansCreated() { return beansCreated; }
        public long getSlowestBeanTimeMs() { return slowestBeanTimeMs; }
        public String getSlowestBeanName() { return slowestBeanName; }
        public double getBeanCreationEfficiency() { return beanCreationEfficiency; }
        public double getMemoryEfficiencyMBPerBean() { return memoryEfficiencyMBPerBean; }
    }
}