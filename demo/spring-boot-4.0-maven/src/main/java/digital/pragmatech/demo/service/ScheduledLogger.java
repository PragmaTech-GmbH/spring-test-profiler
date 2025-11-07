package digital.pragmatech.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextPausedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledLogger {

  private static final Logger LOG = LoggerFactory.getLogger(ScheduledLogger.class);

  private final ApplicationContext applicationContext;

  public ScheduledLogger(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Scheduled(fixedDelay = 100L)
  public void log() {
    LOG.info("Scheduled task executed within context '{}'.", applicationContext.hashCode());
  }

  @EventListener(ContextPausedEvent.class)
  public void contextPaused(ContextPausedEvent event) {
    LOG.info("Application context '{}' has been paused.", event.getApplicationContext().hashCode());
  }
}
