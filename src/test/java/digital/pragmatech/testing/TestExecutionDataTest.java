package digital.pragmatech.testing;

import java.time.Duration;
import java.time.Instant;

import digital.pragmatech.testing.reporting.TestExecutionData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestExecutionDataTest {

  @Test
  void testConstructorAndGetters() {
    Instant startTime = Instant.now();
    TestExecutionData data = new TestExecutionData("com.example.TestClass#testMethod", startTime);

    assertEquals("com.example.TestClass#testMethod", data.getTestId());
    assertEquals(startTime, data.getStartTime());
    assertNull(data.getEndTime());
    assertEquals(TestStatus.RUNNING, data.getStatus());
    assertNull(data.getThrowable());
    assertNull(data.getReason());
  }

  @Test
  void testDurationCalculation() {
    Instant startTime = Instant.now();
    TestExecutionData data = new TestExecutionData("test#method", startTime);

    // Initially no duration
    assertNull(data.getDuration());

    // Set end time and check duration
    Instant endTime = startTime.plusMillis(500);
    data.setEndTime(endTime);

    assertEquals(Duration.ofMillis(500), data.getDuration());
  }

  @Test
  void testTestMethodNameExtraction() {
    TestExecutionData data = new TestExecutionData("com.example.TestClass#testMethod", Instant.now());

    assertEquals("testMethod", data.getTestMethodName());
    assertEquals("com.example.TestClass", data.getTestClassName());
  }

  @Test
  void testTestMethodNameExtractionWithNestedClass() {
    TestExecutionData data = new TestExecutionData("com.example.OuterClass$InnerClass#testMethod", Instant.now());

    assertEquals("testMethod", data.getTestMethodName());
    assertEquals("com.example.OuterClass$InnerClass", data.getTestClassName());
  }

  @Test
  void testStatusTransitions() {
    TestExecutionData data = new TestExecutionData("test#method", Instant.now());

    assertEquals(TestStatus.RUNNING, data.getStatus());

    data.setStatus(TestStatus.PASSED);
    assertEquals(TestStatus.PASSED, data.getStatus());

    data.setStatus(TestStatus.FAILED);
    assertEquals(TestStatus.FAILED, data.getStatus());
  }

  @Test
  void testFailureWithThrowable() {
    TestExecutionData data = new TestExecutionData("test#method", Instant.now());
    Exception exception = new RuntimeException("Test failed");

    data.setStatus(TestStatus.FAILED);
    data.setThrowable(exception);

    assertEquals(TestStatus.FAILED, data.getStatus());
    assertEquals(exception, data.getThrowable());
  }

  @Test
  void testDisabledWithReason() {
    TestExecutionData data = new TestExecutionData("test#method", Instant.now());

    data.setStatus(TestStatus.DISABLED);
    data.setReason("Feature not implemented yet");

    assertEquals(TestStatus.DISABLED, data.getStatus());
    assertEquals("Feature not implemented yet", data.getReason());
  }
}
