package digital.pragmatech.testing.budget;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

/** The single test reported by the {@link ContextBudgetTestEngine}. */
class ContextBudgetTestDescriptor extends AbstractTestDescriptor {

  static final String SEGMENT_TYPE = "context-budget";

  static final String METHOD_NAME = "distinctSpringContextsWithinBudget";

  private final int maxContexts;

  ContextBudgetTestDescriptor(UniqueId parentId, int maxContexts) {
    // Maven Surefire and Failsafe only count and report a test result with a method source
    super(
        parentId.append(SEGMENT_TYPE, "max-contexts"),
        "max " + maxContexts + " contexts",
        MethodSource.from(ContextBudgetTestEngine.class.getName(), METHOD_NAME));
    this.maxContexts = maxContexts;
  }

  int getMaxContexts() {
    return maxContexts;
  }

  @Override
  public Type getType() {
    return Type.TEST;
  }
}
