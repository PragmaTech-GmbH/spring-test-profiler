package digital.pragmatech.testing.optimization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileBasedAnalysisContextTest {

  @TempDir Path tempDir;

  private FileBasedAnalysisContext context;
  private Set<String> testClasses;

  @BeforeEach
  void setUp() {
    testClasses = Set.of("com.example.TestClass1", "com.example.TestClass2");
    context = new FileBasedAnalysisContext(testClasses);
  }

  @Test
  void shouldReturnTestClasses() {
    Set<String> classes = context.getTestClasses();

    assertThat(classes)
        .containsExactlyInAnyOrder("com.example.TestClass1", "com.example.TestClass2");
  }

  @Test
  void shouldReturnProjectRoot() {
    Path projectRoot = context.getProjectRoot();

    assertThat(projectRoot).isNotNull();
    assertThat(projectRoot.toString()).contains(System.getProperty("user.dir"));
  }

  @Test
  void shouldReturnTestSourcePaths() {
    Set<Path> testSourcePaths = context.getTestSourcePaths();

    assertThat(testSourcePaths).isNotEmpty();
    assertThat(testSourcePaths).allMatch(path -> path.toString().contains("src/test/java"));
  }

  @Test
  void shouldReturnEmptyContentForNonExistentClass() {
    String content = context.getClassContent("com.example.NonExistent");

    assertThat(content).isEmpty();
  }

  @Test
  void shouldDetectAnnotationPresence() throws IOException {
    String className = "com.example.TestClass";
    String classContent =
        """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;

            @SpringBootTest
            @DirtiesContext
            public class TestClass {
            }
            """;

    // Create a temporary test file
    Path testSourceDir = tempDir.resolve("src/test/java/com/example");
    Files.createDirectories(testSourceDir);
    Path testFile = testSourceDir.resolve("TestClass.java");
    Files.writeString(testFile, classContent);

    // Override project root for this test
    FileBasedAnalysisContext testContext =
        new TestFileBasedAnalysisContext(Set.of(className), tempDir);

    boolean hasDirtiesContext = testContext.hasAnnotation(className, "DirtiesContext");
    boolean hasSpringBootTest = testContext.hasAnnotation(className, "SpringBootTest");
    boolean hasNonExistent = testContext.hasAnnotation(className, "NonExistentAnnotation");

    assertThat(hasDirtiesContext).isTrue();
    assertThat(hasSpringBootTest).isTrue();
    assertThat(hasNonExistent).isFalse();
  }

  @Test
  void shouldExtractAllAnnotationsForClass() throws IOException {
    String className = "com.example.TestClass";
    String classContent =
        """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.junit.jupiter.api.Test;

            @SpringBootTest
            @DirtiesContext
            public class TestClass {

                @Test
                @SomeOtherAnnotation
                public void testMethod() {
                }
            }
            """;

    Path testSourceDir = tempDir.resolve("src/test/java/com/example");
    Files.createDirectories(testSourceDir);
    Path testFile = testSourceDir.resolve("TestClass.java");
    Files.writeString(testFile, classContent);

    FileBasedAnalysisContext testContext =
        new TestFileBasedAnalysisContext(Set.of(className), tempDir);

    Set<String> annotations = testContext.getAnnotationsForClass(className);

    assertThat(annotations)
        .contains("SpringBootTest", "DirtiesContext", "Test", "SomeOtherAnnotation");
  }

  @Test
  void shouldExtractMethodAnnotations() throws IOException {
    String className = "com.example.TestClass";
    String classContent =
        """
            package com.example;

            import org.springframework.test.annotation.DirtiesContext;
            import org.junit.jupiter.api.Test;

            public class TestClass {

                @Test
                @DirtiesContext
                public void testMethod() {
                }

                @Test
                public void anotherTestMethod() {
                }
            }
            """;

    Path testSourceDir = tempDir.resolve("src/test/java/com/example");
    Files.createDirectories(testSourceDir);
    Path testFile = testSourceDir.resolve("TestClass.java");
    Files.writeString(testFile, classContent);

    FileBasedAnalysisContext testContext =
        new TestFileBasedAnalysisContext(Set.of(className), tempDir);

    Set<String> testMethodAnnotations = testContext.getMethodAnnotations(className, "testMethod");
    Set<String> anotherTestMethodAnnotations =
        testContext.getMethodAnnotations(className, "anotherTestMethod");

    assertThat(testMethodAnnotations).contains("Test", "DirtiesContext");
    assertThat(anotherTestMethodAnnotations).contains("Test");
    assertThat(anotherTestMethodAnnotations).doesNotContain("DirtiesContext");
  }

  // Helper class to override project root for testing
  private static class TestFileBasedAnalysisContext extends FileBasedAnalysisContext {
    private final Path testProjectRoot;

    public TestFileBasedAnalysisContext(Set<String> testClasses, Path testProjectRoot) {
      super(testClasses);
      this.testProjectRoot = testProjectRoot;
    }

    @Override
    public Path getProjectRoot() {
      return testProjectRoot;
    }

    @Override
    public Set<Path> getTestSourcePaths() {
      return Set.of(testProjectRoot.resolve("src/test/java"));
    }
  }
}
