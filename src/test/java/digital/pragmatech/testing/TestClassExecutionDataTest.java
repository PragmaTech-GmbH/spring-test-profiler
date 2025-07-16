package digital.pragmatech.testing;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import digital.pragmatech.testing.reporting.TestClassExecutionData;
import digital.pragmatech.testing.reporting.TestExecutionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestClassExecutionDataTest {

  private Map<String, TestExecutionData> testExecutions;
  private SpringContextStatistics contextStatistics;

  @BeforeEach
  void setUp() {
    testExecutions = new HashMap<>();
    contextStatistics = new SpringContextStatistics();

    // Add some test data
    TestExecutionData test1 = new TestExecutionData("TestClass#test1", Instant.now());
    test1.setStatus(TestStatus.PASSED);
    testExecutions.put(test1.getTestId(), test1);

    TestExecutionData test2 = new TestExecutionData("TestClass#test2", Instant.now());
    test2.setStatus(TestStatus.FAILED);
    testExecutions.put(test2.getTestId(), test2);

    TestExecutionData test3 = new TestExecutionData("TestClass#test3", Instant.now());
    test3.setStatus(TestStatus.DISABLED);
    testExecutions.put(test3.getTestId(), test3);

    TestExecutionData test4 = new TestExecutionData("TestClass#test4", Instant.now());
    test4.setStatus(TestStatus.ABORTED);
    testExecutions.put(test4.getTestId(), test4);

    TestExecutionData test5 = new TestExecutionData("TestClass#test5", Instant.now());
    test5.setStatus(TestStatus.PASSED);
    testExecutions.put(test5.getTestId(), test5);
  }

  @Test
  void testConstructorAndGetters() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals("com.example.TestClass", classData.className());
    assertEquals(testExecutions, classData.testExecutions());
    assertEquals(contextStatistics, classData.contextStatistics());
  }

  @Test
  void testTotalTestsCount() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(5, classData.getTotalTests());
  }

  @Test
  void testPassedTestsCount() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(2, classData.getPassedTests());
  }

  @Test
  void testFailedTestsCount() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(1, classData.getFailedTests());
  }

  @Test
  void testDisabledTestsCount() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(1, classData.getDisabledTests());
  }

  @Test
  void testAbortedTestsCount() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(1, classData.getAbortedTests());
  }

  @Test
  void testEmptyTestExecutions() {
    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.EmptyTestClass",
      new HashMap<>(),
      contextStatistics
    );

    assertEquals(0, classData.getTotalTests());
    assertEquals(0, classData.getPassedTests());
    assertEquals(0, classData.getFailedTests());
    assertEquals(0, classData.getDisabledTests());
    assertEquals(0, classData.getAbortedTests());
  }

  @Test
  void testWithContextStatistics() {
    // Add some context statistics
    contextStatistics.recordContextLoad("test-context", java.time.Duration.ofMillis(1000));
    contextStatistics.recordCacheHit("test-context");

    TestClassExecutionData classData = new TestClassExecutionData(
      "com.example.TestClass",
      testExecutions,
      contextStatistics
    );

    assertEquals(1, classData.contextStatistics().getContextLoads());
    assertEquals(1, classData.contextStatistics().getCacheHits());
  }
}
