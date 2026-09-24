package digital.pragmatech.testing.budget;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Tracks which test engines of the current test plan have finished.
 *
 * <p>JUnit Platform executes test engines one after the other in {@code ServiceLoader} (classpath)
 * order. The {@link ContextBudgetTestEngine} can only see the final context count when it runs
 * after all other engines. This listener lets the engine detect when that is not the case, so it
 * can fail with a clear message instead of checking an incomplete count.
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}.
 */
public class ContextBudgetOrderGuard implements TestExecutionListener {

  private static final Object lock = new Object();

  // null when no test plan was seen, e.g. when the engine is run by the EngineTestKit
  private static Set<String> pendingEngineIds;

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    Set<String> engineIds = new LinkedHashSet<>();
    for (TestIdentifier root : testPlan.getRoots()) {
      engineIds.add(root.getUniqueId());
    }
    synchronized (lock) {
      pendingEngineIds = engineIds;
    }
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
    if (testIdentifier.getParentId().isPresent()) {
      return;
    }
    synchronized (lock) {
      if (pendingEngineIds != null) {
        pendingEngineIds.remove(testIdentifier.getUniqueId());
      }
    }
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    synchronized (lock) {
      pendingEngineIds = null;
    }
  }

  /**
   * Gets the ids of engines in the current test plan that did not finish yet, excluding the given
   * engine. Returns an empty list when no test plan is known.
   */
  static List<String> getUnfinishedEnginesExcept(String ownEngineUniqueId) {
    synchronized (lock) {
      if (pendingEngineIds == null) {
        return List.of();
      }
      return pendingEngineIds.stream().filter(id -> !id.equals(ownEngineUniqueId)).toList();
    }
  }

  static void reset() {
    synchronized (lock) {
      pendingEngineIds = null;
    }
  }

  static void simulateTestPlanStarted(Set<String> engineUniqueIds) {
    synchronized (lock) {
      pendingEngineIds = new LinkedHashSet<>(engineUniqueIds);
    }
  }
}
