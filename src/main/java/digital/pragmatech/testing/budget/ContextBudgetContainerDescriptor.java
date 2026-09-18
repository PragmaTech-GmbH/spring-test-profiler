package digital.pragmatech.testing.budget;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

/**
 * Container for the context budget test. Build tools like Maven Surefire and Failsafe only report
 * tests that belong to a container with a {@link ClassSource}; without it, a failed budget check
 * would not fail the build.
 */
class ContextBudgetContainerDescriptor extends AbstractTestDescriptor {

  static final String SEGMENT_TYPE = "context-budget-container";

  ContextBudgetContainerDescriptor(UniqueId engineId) {
    super(
        engineId.append(SEGMENT_TYPE, "spring-context-budget"),
        "Spring context budget",
        ClassSource.from(ContextBudgetTestEngine.class));
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
  }
}
