package digital.pragmatech.testing;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringTestProfilerExtension implements BeforeAllCallback, AfterAllCallback, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(SpringTestProfilerExtension.class);
  private static final String STORE_KEY = "spring-test-profiler";

  private static volatile boolean reportGenerated = false;

  private static BuildToolDetection.ExecutionEnvironment detectedEnvironment;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    logger.debug("Starting Spring Test Insight for test class: {}", testClass.getName());

    // Detect execution environment using enhanced detection approach
    if (detectedEnvironment == null) {
      detectedEnvironment = BuildToolDetection.detectBuildToolPhase(testClass);
      logger.info("Detected execution environment: {}", detectedEnvironment);
    }

    getStore(context).put(STORE_KEY, this);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    logger.debug("Completing Spring Test Insight for test class: {}", context.getRequiredTestClass().getName());
    // Report generation is now handled in the close() method
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
  }

  @Override
  public void close() {
    // This method is called after all tests have completed
    // Use synchronization to ensure the report is only generated once per phase
    synchronized (SpringTestProfilerExtension.class) {
      if (!reportGenerated) {
        String phase = detectedEnvironment.toString();
        logger.info("All tests completed for {} phase. Generating Spring Test Insight report...", phase);

        // Call the SpringTestInsightListener to generate the report
        SpringTestInsightListener.generateReport(phase);
        reportGenerated = true;
      }
      else {
        logger.debug("Report already generated for current phase, skipping...");
      }
    }
  }
}
