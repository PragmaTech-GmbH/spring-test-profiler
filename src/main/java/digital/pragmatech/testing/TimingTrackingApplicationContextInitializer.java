package digital.pragmatech.testing;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Enhanced ApplicationContextInitializer that provides comprehensive profiling of Spring context loading.
 * Tracks timing, memory usage, bean creation, and lifecycle events from within the Spring context loading process.
 */
@Order(HIGHEST_PRECEDENCE)
public class TimingTrackingApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final Logger logger = LoggerFactory.getLogger(TimingTrackingApplicationContextInitializer.class);

  // Thread-safe maps for context tracking
  private static final Map<ConfigurableApplicationContext, Instant> contextStartTimes = new ConcurrentHashMap<>();
  private static final Map<String, Long> contextLoadTimes = new ConcurrentHashMap<>();
  private static final Map<String, ContextProfileData> contextProfileData = new ConcurrentHashMap<>();

  // Memory tracking
  private static final Map<ConfigurableApplicationContext, Long> contextStartMemory = new ConcurrentHashMap<>();

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    // Capture start time and memory state
    Instant startTime = Instant.now();
    long startMemory = getUsedMemory();

    String contextId = generateContextId(applicationContext);

    // Store start state
    contextStartTimes.put(applicationContext, startTime);
    contextStartMemory.put(applicationContext, startMemory);

    // Initialize profile data
    ContextProfileData profileData = new ContextProfileData(contextId, startTime, startMemory);
    contextProfileData.put(contextId, profileData);

    logger.debug("Context profiling started for {} (start memory: {}MB)", contextId, startMemory / 1024 / 1024);

    // Add bean creation profiler
    BeanCreationProfiler beanProfiler = new BeanCreationProfiler(contextId);
    applicationContext.getBeanFactory().addBeanPostProcessor(beanProfiler);

    // Add BeanFactory post-processor for early profiling
    applicationContext.addBeanFactoryPostProcessor(beanFactory -> {
      profileData.setBeanDefinitionCount(beanFactory.getBeanDefinitionCount());
      profileData.recordPhase("BeanDefinitionRegistration", Instant.now());
      logger.debug("Registered {} bean definitions for context {}",
        beanFactory.getBeanDefinitionCount(), contextId);
    });

    // Add comprehensive lifecycle listener
    applicationContext.addApplicationListener(event -> {
      handleContextEvent(event, applicationContext, contextId, profileData, beanProfiler);
    });
  }

  private void handleContextEvent(Object event, ConfigurableApplicationContext applicationContext,
    String contextId, ContextProfileData profileData, BeanCreationProfiler beanProfiler) {
    if (event instanceof ApplicationContextEvent) {
      ApplicationContextEvent contextEvent = (ApplicationContextEvent) event;

      if (contextEvent.getSource() == applicationContext) {
        String eventType = event.getClass().getSimpleName();
        profileData.recordPhase(eventType, Instant.now());

        if (event instanceof ContextRefreshedEvent) {
          // Context loading completed - finalize profiling
          finalizeContextProfiling(applicationContext, contextId, profileData, beanProfiler);
        }

        logger.debug("Context event {} for {}", eventType, contextId);
      }
    }
  }

  private void finalizeContextProfiling(ConfigurableApplicationContext applicationContext, String contextId,
    ContextProfileData profileData, BeanCreationProfiler beanProfiler) {
    Instant endTime = Instant.now();
    long endMemory = getUsedMemory();

    Instant startTime = contextStartTimes.remove(applicationContext);
    Long startMemoryValue = contextStartMemory.remove(applicationContext);

    if (startTime != null) {
      long loadTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
      long memoryUsed = endMemory - (startMemoryValue != null ? startMemoryValue : 0);

      // Finalize profile data
      profileData.setEndTime(endTime);
      profileData.setEndMemory(endMemory);
      profileData.setTotalLoadTimeMs(loadTimeMs);
      profileData.setMemoryUsedMB(memoryUsed / 1024 / 1024);
      profileData.setBeanCreationMetrics(beanProfiler.getMetrics());

      // Store for retrieval
      contextLoadTimes.put(contextId, loadTimeMs);

      logger.info("Context {} loaded in {}ms (memory: +{}MB, beans: {})",
        contextId, loadTimeMs, memoryUsed / 1024 / 1024, beanProfiler.getMetrics().getTotalBeansCreated());
    }
  }

  private String generateContextId(ConfigurableApplicationContext context) {
    return context.getClass().getSimpleName() + "@" + System.identityHashCode(context);
  }

  private long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  public static ContextProfileData getContextProfileData(ConfigurableApplicationContext context) {
    String contextId = context.getClass().getSimpleName() + "@" + System.identityHashCode(context);
    return contextProfileData.get(contextId);
  }
}
