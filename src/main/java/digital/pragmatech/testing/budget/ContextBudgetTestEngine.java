package digital.pragmatech.testing.budget;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.SpringTestProfilerListener;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * JUnit Platform test engine that fails the build when the number of distinct Spring test contexts
 * is above a configured limit.
 *
 * <p>Activated by the configuration parameter {@value #MAX_CONTEXTS_PROPERTY} (JUnit configuration
 * parameter, system property, or {@code junit-platform.properties}). Without it, the engine
 * discovers no tests and stays invisible.
 *
 * <p>JUnit Platform runs engines one after the other, so when this engine executes, the JUnit
 * Jupiter engine (also with parallel execution) has finished and the context count is final for
 * this JVM.
 */
public class ContextBudgetTestEngine implements TestEngine {

  public static final String ENGINE_ID = "spring-test-profiler-context-budget";

  public static final String MAX_CONTEXTS_PROPERTY = "spring.test.profiler.max-contexts";

  private final Supplier<List<ContextCacheEntry>> createdContextEntriesSupplier;

  public ContextBudgetTestEngine() {
    this(SpringTestProfilerListener::getCreatedContextEntries);
  }

  ContextBudgetTestEngine(Supplier<List<ContextCacheEntry>> createdContextEntriesSupplier) {
    this.createdContextEntriesSupplier = createdContextEntriesSupplier;
  }

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  public Optional<String> getGroupId() {
    return Optional.of("digital.pragmatech.testing");
  }

  @Override
  public Optional<String> getArtifactId() {
    return Optional.of("spring-test-profiler");
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "Spring Test Profiler");
    discoveryRequest
        .getConfigurationParameters()
        .get(MAX_CONTEXTS_PROPERTY)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(ContextBudgetTestEngine::parseMaxContexts)
        .ifPresent(
            maxContexts -> {
              ContextBudgetContainerDescriptor containerDescriptor =
                  new ContextBudgetContainerDescriptor(uniqueId);
              containerDescriptor.addChild(
                  new ContextBudgetTestDescriptor(containerDescriptor.getUniqueId(), maxContexts));
              engineDescriptor.addChild(containerDescriptor);
            });
    return engineDescriptor;
  }

  @Override
  public void execute(ExecutionRequest request) {
    TestDescriptor engineDescriptor = request.getRootTestDescriptor();
    EngineExecutionListener listener = request.getEngineExecutionListener();

    listener.executionStarted(engineDescriptor);
    for (TestDescriptor containerDescriptor : engineDescriptor.getChildren()) {
      listener.executionStarted(containerDescriptor);
      for (TestDescriptor child : containerDescriptor.getChildren()) {
        if (child instanceof ContextBudgetTestDescriptor budgetDescriptor) {
          listener.executionStarted(budgetDescriptor);
          listener.executionFinished(
              budgetDescriptor,
              checkBudget(engineDescriptor.getUniqueId(), budgetDescriptor.getMaxContexts()));
        }
      }
      listener.executionFinished(containerDescriptor, TestExecutionResult.successful());
    }
    listener.executionFinished(engineDescriptor, TestExecutionResult.successful());
  }

  private TestExecutionResult checkBudget(UniqueId engineUniqueId, int maxContexts) {
    List<String> unfinishedEngines =
        ContextBudgetOrderGuard.getUnfinishedEnginesExcept(engineUniqueId.toString());
    if (!unfinishedEngines.isEmpty()) {
      return TestExecutionResult.failed(
          new IllegalStateException(
              "The Spring context budget check ran before these test engines finished: "
                  + unfinishedEngines
                  + ". The context count is not final. Place the spring-test-profiler dependency"
                  + " after junit-jupiter (e.g. spring-boot-starter-test) on the test classpath."));
    }

    List<ContextCacheEntry> createdEntries = createdContextEntriesSupplier.get();
    if (createdEntries.size() <= maxContexts) {
      return TestExecutionResult.successful();
    }
    return TestExecutionResult.failed(
        new AssertionError(buildFailureMessage(maxContexts, createdEntries)));
  }

  static String buildFailureMessage(int maxContexts, List<ContextCacheEntry> createdEntries) {
    StringBuilder message = new StringBuilder();
    message
        .append("Spring context budget exceeded: ")
        .append(createdEntries.size())
        .append(" distinct contexts were created, but the limit is ")
        .append(maxContexts)
        .append(".\nHarmonize the test configuration to reuse an existing context, or raise '")
        .append(MAX_CONTEXTS_PROPERTY)
        .append("' on purpose.\n\nContexts and their test classes:");

    List<ContextCacheEntry> sortedEntries =
        createdEntries.stream()
            .sorted(
                Comparator.comparing(
                    ContextCacheEntry::getCreationTime,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    int contextNumber = 1;
    for (ContextCacheEntry entry : sortedEntries) {
      message.append("\n  ").append(contextNumber++).append(". ");
      message.append(entry.getTestClasses().stream().sorted().toList());
    }
    return message.toString();
  }

  private static int parseMaxContexts(String value) {
    try {
      int maxContexts = Integer.parseInt(value);
      if (maxContexts < 0) {
        throw new IllegalArgumentException(
            MAX_CONTEXTS_PROPERTY + " must not be negative, but was: " + value);
      }
      return maxContexts;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          MAX_CONTEXTS_PROPERTY + " must be a whole number, but was: " + value, e);
    }
  }
}
