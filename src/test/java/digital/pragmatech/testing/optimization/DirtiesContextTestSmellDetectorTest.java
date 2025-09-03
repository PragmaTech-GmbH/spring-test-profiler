package digital.pragmatech.testing.optimization;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DirtiesContextTestSmellDetectorTest {

  private DirtiesContextTestSmellDetector detector;
  private TestStaticAnalysisContext mockContext;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    detector = new DirtiesContextTestSmellDetector();
    mockContext = new TestStaticAnalysisContext();
  }

  @Test
  void shouldReturnCorrectTestSmellType() {
    assertThat(detector.getTestSmellType()).isEqualTo("DIRTIES_CONTEXT_USAGE");
  }

  @Test
  void shouldReturnCorrectDescription() {
    assertThat(detector.getDescription())
        .isEqualTo(
            "Detects usage of @DirtiesContext annotation which prevents Spring context caching");
  }

  @Test
  void shouldDetectClassLevelDirtiesContext() throws Exception {
    String testClass = "com.example.TestWithClassLevelDirtiesContext";
    String classContent = createClassWithClassLevelDirtiesContext();

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, classContent);

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).hasSize(1);
    OptimizationRecord record = records.get(0);
    assertThat(record.testClass()).isEqualTo(testClass);
    assertThat(record.testSmellType()).isEqualTo("DIRTIES_CONTEXT_USAGE");
    assertThat(record.title()).contains("@DirtiesContext detected (class-level)");
    assertThat(record.severity()).isEqualTo(OptimizationRecord.Severity.HIGH);
    assertThat(record.description()).contains("prevents Spring context caching");
    assertThat(record.recommendation()).contains("Remove @DirtiesContext");
  }

  @Test
  void shouldDetectMethodLevelDirtiesContext() throws Exception {
    String testClass = "com.example.TestWithMethodLevelDirtiesContext";
    String classContent = createClassWithMethodLevelDirtiesContext();

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, classContent);

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).hasSize(1);
    OptimizationRecord record = records.get(0);
    assertThat(record.testClass()).isEqualTo(testClass);
    assertThat(record.title()).contains("@DirtiesContext detected (method-level on testMethod)");
    assertThat(record.severity()).isEqualTo(OptimizationRecord.Severity.MEDIUM);
  }

  @Test
  void shouldDetectMultipleMethodLevelDirtiesContext() throws Exception {
    String testClass = "com.example.TestWithMultipleMethodLevelDirtiesContext";
    String classContent = createClassWithMultipleMethodLevelDirtiesContext();

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, classContent);

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).hasSize(2);
    assertThat(records).allMatch(r -> r.testClass().equals(testClass));
    assertThat(records).anyMatch(r -> r.title().contains("testMethod1"));
    assertThat(records).anyMatch(r -> r.title().contains("testMethod2"));
  }

  @Test
  void shouldNotDetectWhenNoDirtiesContext() throws Exception {
    String testClass = "com.example.CleanTest";
    String classContent = createCleanTestClass();

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, classContent);

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).isEmpty();
  }

  @Test
  void shouldHandleEmptyClassContent() throws Exception {
    String testClass = "com.example.EmptyTest";

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, "");

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).isEmpty();
  }

  @Test
  void shouldDetectBothClassAndMethodLevel() throws Exception {
    String testClass = "com.example.TestWithBothLevels";
    String classContent = createClassWithBothLevelDirtiesContext();

    mockContext.addTestClass(testClass);
    mockContext.setClassContent(testClass, classContent);

    List<OptimizationRecord> records = detector.analyze(mockContext);

    assertThat(records).hasSize(2);
    assertThat(records).anyMatch(r -> r.title().contains("class-level"));
    assertThat(records).anyMatch(r -> r.title().contains("method-level"));
  }

  private String createClassWithClassLevelDirtiesContext() {
    return """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            @DirtiesContext
            public class TestWithClassLevelDirtiesContext {

                @Test
                void testMethod() {
                    // test implementation
                }
            }
            """;
  }

  private String createClassWithMethodLevelDirtiesContext() {
    return """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            public class TestWithMethodLevelDirtiesContext {

                @Test
                @DirtiesContext
                void testMethod() {
                    // test implementation
                }
            }
            """;
  }

  private String createClassWithMultipleMethodLevelDirtiesContext() {
    return """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            public class TestWithMultipleMethodLevelDirtiesContext {

                @Test
                @DirtiesContext
                void testMethod1() {
                    // test implementation
                }

                @Test
                @DirtiesContext
                void testMethod2() {
                    // test implementation
                }
            }
            """;
  }

  private String createCleanTestClass() {
    return """
            package com.example;

            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            public class CleanTest {

                @Test
                void testMethod() {
                    // test implementation
                }
            }
            """;
  }

  private String createClassWithBothLevelDirtiesContext() {
    return """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            @DirtiesContext
            public class TestWithBothLevels {

                @Test
                @DirtiesContext
                void testMethod() {
                    // test implementation
                }
            }
            """;
  }
}
