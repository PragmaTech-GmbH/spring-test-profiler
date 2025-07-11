package digital.pragmatech.springtestinsight.experiment;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ExecutionContextListener implements TestExecutionListener {
  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    // Access all selected test classes through the test plan
    testPlan.getRoots().forEach(root -> {
      testPlan.getDescendants(root).forEach(testId -> {
        System.out.println("Selected test: " + testId.getDisplayName());
      });
    });

    // Access configuration parameters (JUnit Platform 1.8+)
    testPlan.getConfigurationParameters().get("execution.environment")
      .ifPresent(env -> System.out.println("Execution environment: " + env));
  }
}
