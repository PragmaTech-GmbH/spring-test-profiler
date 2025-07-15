package digital.pragmatech.testing;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * BeanPostProcessor that tracks detailed bean creation metrics during context loading.
 * Provides insights into bean creation timing, order, and dependencies.
 */
public class BeanCreationProfiler implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BeanCreationProfiler.class);
    
    private final String contextId;
    private final Map<String, BeanCreationMetric> beanMetrics = new ConcurrentHashMap<>();
    private final Map<String, Instant> beanStartTimes = new ConcurrentHashMap<>();
    private final AtomicLong beanCreationOrder = new AtomicLong(0);
    
    // Aggregated metrics
    private final AtomicLong totalBeansCreated = new AtomicLong(0);
    private final AtomicLong totalCreationTimeMs = new AtomicLong(0);
    private volatile long slowestBeanTimeMs = 0;
    private volatile String slowestBeanName = null;

    public BeanCreationProfiler(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // Record bean creation start
        beanStartTimes.put(beanName, Instant.now());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // Record bean creation completion
        Instant endTime = Instant.now();
        Instant startTime = beanStartTimes.remove(beanName);
        
        if (startTime != null) {
            long creationTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            long order = beanCreationOrder.incrementAndGet();
            
            // Create metric record
            BeanCreationMetric metric = new BeanCreationMetric(
                beanName,
                bean.getClass().getName(),
                startTime,
                endTime,
                creationTimeMs,
                order
            );
            
            beanMetrics.put(beanName, metric);
            
            // Update aggregated metrics
            totalBeansCreated.incrementAndGet();
            totalCreationTimeMs.addAndGet(creationTimeMs);
            
            // Track slowest bean
            if (creationTimeMs > slowestBeanTimeMs) {
                slowestBeanTimeMs = creationTimeMs;
                slowestBeanName = beanName;
            }
            
            // Log slow beans
            if (creationTimeMs > 100) {
                logger.debug("Slow bean creation: {} took {}ms (order: {})", beanName, creationTimeMs, order);
            }
        }
        
        return bean;
    }
    
    /**
     * Gets comprehensive bean creation metrics.
     */
    public BeanCreationMetrics getMetrics() {
        return new BeanCreationMetrics(
            contextId,
            totalBeansCreated.get(),
            totalCreationTimeMs.get(),
            slowestBeanName,
            slowestBeanTimeMs,
            new ArrayList<>(beanMetrics.values())
        );
    }
    
    /**
     * Gets creation metric for a specific bean.
     */
    public BeanCreationMetric getBeanMetric(String beanName) {
        return beanMetrics.get(beanName);
    }
    
    /**
     * Gets beans sorted by creation time (slowest first).
     */
    public List<BeanCreationMetric> getSlowestBeans(int limit) {
        return beanMetrics.values().stream()
            .sorted((a, b) -> Long.compare(b.getCreationTimeMs(), a.getCreationTimeMs()))
            .limit(limit)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Individual bean creation metric.
     */
    public static class BeanCreationMetric {
        private final String beanName;
        private final String beanClass;
        private final Instant startTime;
        private final Instant endTime;
        private final long creationTimeMs;
        private final long creationOrder;
        
        public BeanCreationMetric(String beanName, String beanClass, Instant startTime, 
                                 Instant endTime, long creationTimeMs, long creationOrder) {
            this.beanName = beanName;
            this.beanClass = beanClass;
            this.startTime = startTime;
            this.endTime = endTime;
            this.creationTimeMs = creationTimeMs;
            this.creationOrder = creationOrder;
        }
        
        public String getBeanName() { return beanName; }
        public String getBeanClass() { return beanClass; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public long getCreationTimeMs() { return creationTimeMs; }
        public long getCreationOrder() { return creationOrder; }
    }
    
    /**
     * Aggregated bean creation metrics for a context.
     */
    public static class BeanCreationMetrics {
        private final String contextId;
        private final long totalBeansCreated;
        private final long totalCreationTimeMs;
        private final String slowestBeanName;
        private final long slowestBeanTimeMs;
        private final List<BeanCreationMetric> allBeans;
        
        public BeanCreationMetrics(String contextId, long totalBeansCreated, long totalCreationTimeMs,
                                  String slowestBeanName, long slowestBeanTimeMs, List<BeanCreationMetric> allBeans) {
            this.contextId = contextId;
            this.totalBeansCreated = totalBeansCreated;
            this.totalCreationTimeMs = totalCreationTimeMs;
            this.slowestBeanName = slowestBeanName;
            this.slowestBeanTimeMs = slowestBeanTimeMs;
            this.allBeans = Collections.unmodifiableList(allBeans);
        }
        
        public String getContextId() { return contextId; }
        public long getTotalBeansCreated() { return totalBeansCreated; }
        public long getTotalCreationTimeMs() { return totalCreationTimeMs; }
        public String getSlowestBeanName() { return slowestBeanName; }
        public long getSlowestBeanTimeMs() { return slowestBeanTimeMs; }
        public List<BeanCreationMetric> getAllBeans() { return allBeans; }
        
        public double getAverageCreationTimeMs() {
            return totalBeansCreated > 0 ? (double) totalCreationTimeMs / totalBeansCreated : 0.0;
        }
    }
}