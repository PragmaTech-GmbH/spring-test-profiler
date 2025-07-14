package digital.pragmatech.testing;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;

public class SpringTestProfilerExtension implements BeforeAllCallback, AfterAllCallback {

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

    synchronized (SpringTestProfilerExtension.class) {
      if (!reportGenerated) {
        String phase = detectedEnvironment.toString();
        logger.debug("All tests completed for {} phase. Generating Spring Test Insight report...", phase);

        // Call the SpringTestInsightListener to generate the report
        SpringTestInsightListener.generateReport(phase);
        reportGenerated = true;
      }
      else {
        logger.debug("Report already generated for current phase, skipping...");
      }
    }
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
  }
}
