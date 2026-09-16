package digital.pragmatech.testing.budget;

import java.time.Instant;
import java.util.Set;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.ContextRemovalReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.springframework.test.context.MergedContextConfiguration;

import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

class ContextBudgetTestEngineTest {

  private ContextCacheTracker contextCacheTracker;

  @BeforeEach
  void setUp() {
    contextCacheTracker = new ContextCacheTracker();
    ContextBudgetOrderGuard.reset();
  }

  @AfterEach
  void tearDown() {
    ContextBudgetOrderGuard.reset();
  }

  @Test
  void shouldDiscoverNoTestsWhenLimitIsNotConfigured() {
    EngineExecutionResults results = EngineTestKit.engine(createEngine()).execute();

    results.testEvents().assertStatistics(stats -> stats.started(0));
  }

  @Test
  void shouldSucceedWhenDistinctContextCountIsWithinLimit() {
    recordCreatedContext(Object.class, "com.example.TestA");
    recordCreatedContext(String.class, "com.example.TestB");

    EngineExecutionResults results = executeWithLimit("2");

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
  }

  @Test
  void shouldNotCountRecreatedContextTwice() {
    MergedContextConfiguration config = recordCreatedContext(Object.class, "com.example.TestA");
    contextCacheTracker.recordContextRemoval(
        config, Instant.now(), ContextRemovalReason.DIRTIES_CONTEXT);
    contextCacheTracker.recordTestClassForContext(config, "com.example.TestB");
    contextCacheTracker.recordContextCreation(config, 100);

    EngineExecutionResults results = executeWithLimit("1");

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  @Test
  void shouldFailAndListTestClassesWhenLimitIsExceeded() {
    recordCreatedContext(Object.class, "com.example.TestA");
    recordCreatedContext(String.class, "com.example.TestB");

    EngineExecutionResults results = executeWithLimit("1");

    results
        .testEvents()
        .assertThatEvents()
        .haveExactly(
            1,
            event(
                test(),
                finishedWithFailure(
                    instanceOf(AssertionError.class),
                    message(
                        text ->
                            text.contains("2 distinct contexts were created, but the limit is 1")
                                && text.contains("com.example.TestA")
                                && text.contains("com.example.TestB")))));
  }

  @Test
  void shouldFailWhenOtherEnginesHaveNotFinished() {
    ContextBudgetOrderGuard.simulateTestPlanStarted(
        Set.of("[engine:junit-jupiter]", "[engine:" + ContextBudgetTestEngine.ENGINE_ID + "]"));

    EngineExecutionResults results = executeWithLimit("5");

    results
        .testEvents()
        .assertThatEvents()
        .haveExactly(
            1,
            event(
                test(),
                finishedWithFailure(
                    instanceOf(IllegalStateException.class),
                    message(
                        text ->
                            text.contains("[engine:junit-jupiter]")
                                && !text.contains(ContextBudgetTestEngine.ENGINE_ID)))));
  }

  private EngineExecutionResults executeWithLimit(String maxContexts) {
    return EngineTestKit.engine(createEngine())
        .configurationParameter(ContextBudgetTestEngine.MAX_CONTEXTS_PROPERTY, maxContexts)
        .execute();
  }

  private ContextBudgetTestEngine createEngine() {
    return new ContextBudgetTestEngine(contextCacheTracker::getCreatedContextEntries);
  }

  private MergedContextConfiguration recordCreatedContext(Class<?> configClass, String testClass) {
    MergedContextConfiguration config =
        new MergedContextConfiguration(configClass, null, new Class<?>[] {configClass}, null, null);
    contextCacheTracker.recordTestClassForContext(config, testClass);
    contextCacheTracker.recordContextCreation(config, 100);
    return config;
  }
}
