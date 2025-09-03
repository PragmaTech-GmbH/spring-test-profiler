package digital.pragmatech.testing.optimization;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticAnalysisEngineTest {

  private TestStaticAnalysisContext testContext;

  @BeforeEach
  void setUp() {
    testContext = new TestStaticAnalysisContext();
  }

  @Test
  void shouldReturnEmptyListWhenNoDetectors() {
    StaticAnalysisEngine emptyEngine = new StaticAnalysisEngine(List.of());

    List<OptimizationRecord> results = emptyEngine.analyze(testContext);

    assertThat(results).isEmpty();
  }

  @Test
  void shouldRunSingleDetector() {
    TestSmellDetector testDetector = new TestTestSmellDetector();
    StaticAnalysisEngine engine = new StaticAnalysisEngine(List.of(testDetector));

    testContext.addTestClass("com.example.TestClass");
    testContext.setClassContent("com.example.TestClass", "test content");

    List<OptimizationRecord> results = engine.analyze(testContext);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).testClass()).isEqualTo("com.example.TestClass");
    assertThat(results.get(0).testSmellType()).isEqualTo("TEST_DETECTOR");
  }

  @Test
  void shouldRunMultipleDetectors() {
    TestSmellDetector detector1 = new TestTestSmellDetector();
    TestSmellDetector detector2 = new AnotherTestSmellDetector();
    StaticAnalysisEngine engine = new StaticAnalysisEngine(List.of(detector1, detector2));

    testContext.addTestClass("com.example.TestClass");
    testContext.setClassContent("com.example.TestClass", "test content");

    List<OptimizationRecord> results = engine.analyze(testContext);

    assertThat(results).hasSize(2);
    assertThat(results).anyMatch(r -> r.testSmellType().equals("TEST_DETECTOR"));
    assertThat(results).anyMatch(r -> r.testSmellType().equals("ANOTHER_DETECTOR"));
  }

  @Test
  void shouldCreateEngineWithDefaultDetectors() {
    StaticAnalysisEngine defaultEngine = new StaticAnalysisEngine();

    List<TestSmellDetector> detectors = defaultEngine.getDetectors();

    // Should have at least the DirtiesContextTestSmellDetector
    assertThat(detectors).isNotEmpty();
    assertThat(detectors).anyMatch(d -> d instanceof DirtiesContextTestSmellDetector);
  }

  @Test
  void shouldHandleDetectorExceptions() {
    TestSmellDetector faultyDetector = new FaultyTestSmellDetector();
    TestSmellDetector workingDetector = new TestTestSmellDetector();
    StaticAnalysisEngine engine =
        new StaticAnalysisEngine(List.of(faultyDetector, workingDetector));

    testContext.addTestClass("com.example.TestClass");
    testContext.setClassContent("com.example.TestClass", "test content");

    List<OptimizationRecord> results = engine.analyze(testContext);

    // Should only have results from the working detector
    assertThat(results).hasSize(1);
    assertThat(results.get(0).testSmellType()).isEqualTo("TEST_DETECTOR");
  }

  // Test helper classes
  private static class TestTestSmellDetector implements TestSmellDetector {
    @Override
    public String getTestSmellType() {
      return "TEST_DETECTOR";
    }

    @Override
    public String getDescription() {
      return "Test detector";
    }

    @Override
    public List<OptimizationRecord> analyze(StaticAnalysisContext context) {
      return context.getTestClasses().stream()
          .map(
              className ->
                  new OptimizationRecord(
                      className,
                      getTestSmellType(),
                      "Test optimization",
                      "Test description",
                      "Test recommendation",
                      OptimizationRecord.Severity.MEDIUM,
                      className + ":1"))
          .toList();
    }
  }

  private static class AnotherTestSmellDetector implements TestSmellDetector {
    @Override
    public String getTestSmellType() {
      return "ANOTHER_DETECTOR";
    }

    @Override
    public String getDescription() {
      return "Another test detector";
    }

    @Override
    public List<OptimizationRecord> analyze(StaticAnalysisContext context) {
      return context.getTestClasses().stream()
          .map(
              className ->
                  new OptimizationRecord(
                      className,
                      getTestSmellType(),
                      "Another optimization",
                      "Another description",
                      "Another recommendation",
                      OptimizationRecord.Severity.LOW,
                      className + ":2"))
          .toList();
    }
  }

  private static class FaultyTestSmellDetector implements TestSmellDetector {
    @Override
    public String getTestSmellType() {
      return "FAULTY_DETECTOR";
    }

    @Override
    public String getDescription() {
      return "Faulty detector";
    }

    @Override
    public List<OptimizationRecord> analyze(StaticAnalysisContext context) {
      throw new RuntimeException("Detector failed");
    }
  }
}
