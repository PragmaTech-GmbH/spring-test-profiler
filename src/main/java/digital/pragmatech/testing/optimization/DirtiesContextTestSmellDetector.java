package digital.pragmatech.testing.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirtiesContextTestSmellDetector implements TestSmellDetector {

  private static final String TEST_SMELL_TYPE = "DIRTIES_CONTEXT_USAGE";
  private static final String ANNOTATION_NAME = "DirtiesContext";
  private static final String FULL_ANNOTATION_NAME =
      "org.springframework.test.annotation.DirtiesContext";

  @Override
  public String getTestSmellType() {
    return TEST_SMELL_TYPE;
  }

  @Override
  public String getDescription() {
    return "Detects usage of @DirtiesContext annotation which prevents Spring context caching";
  }

  @Override
  public List<OptimizationRecord> analyze(StaticAnalysisContext context) {
    List<OptimizationRecord> optimizations = new ArrayList<>();

    for (String testClass : context.getTestClasses()) {
      analyzeClass(testClass, context, optimizations);
    }

    return optimizations;
  }

  private void analyzeClass(
      String testClass, StaticAnalysisContext context, List<OptimizationRecord> optimizations) {
    String classContent = context.getClassContent(testClass);
    if (classContent.isEmpty()) {
      return;
    }

    // Check for class-level @DirtiesContext
    if (hasClassLevelDirtiesContext(classContent)) {
      String sourceLocation = findAnnotationLocation(classContent, testClass, true);
      optimizations.add(createOptimizationRecord(testClass, "class-level", sourceLocation));
    }

    // Check for method-level @DirtiesContext
    List<String> methodsWithDirtiesContext = findMethodsWithDirtiesContext(classContent);
    for (String methodName : methodsWithDirtiesContext) {
      String sourceLocation = findAnnotationLocation(classContent, methodName, false);
      optimizations.add(
          createOptimizationRecord(testClass, "method-level on " + methodName, sourceLocation));
    }
  }

  private boolean hasClassLevelDirtiesContext(String classContent) {
    // Look for @DirtiesContext before class declaration
    Pattern classPattern =
        Pattern.compile(
            "(?:@[^\\n]*\\s+)*@" + ANNOTATION_NAME + ".*?\\s+(?:public\\s+)?class\\s+\\w+",
            Pattern.DOTALL | Pattern.MULTILINE);
    return classPattern.matcher(classContent).find();
  }

  private List<String> findMethodsWithDirtiesContext(String classContent) {
    List<String> methods = new ArrayList<>();

    // Pattern to find methods with @DirtiesContext annotation
    Pattern pattern =
        Pattern.compile(
            "(?:@[^\\n]*\\s+)*@"
                + ANNOTATION_NAME
                + "[^\\n]*\\s+(?:[^\\n]*\\s+)*"
                + "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*\\w+\\s+(\\w+)\\s*\\(",
            Pattern.MULTILINE | Pattern.DOTALL);

    Matcher matcher = pattern.matcher(classContent);
    while (matcher.find()) {
      String methodName = matcher.group(1);
      if (methodName != null && !methodName.isEmpty()) {
        methods.add(methodName);
      }
    }

    return methods;
  }

  private String findAnnotationLocation(String classContent, String target, boolean isClass) {
    String[] lines = classContent.split("\\n");
    String searchPattern =
        isClass ? "class\\s+" + target.substring(target.lastIndexOf('.') + 1) : target;

    for (int i = 0; i < lines.length; i++) {
      if (lines[i].contains("@" + ANNOTATION_NAME)) {
        // Look ahead for the class or method declaration
        for (int j = i; j < Math.min(i + 5, lines.length); j++) {
          if (Pattern.compile(searchPattern).matcher(lines[j]).find()) {
            return target + ":" + (i + 1); // Line numbers are 1-based
          }
        }
      }
    }

    return target + ":unknown";
  }

  private OptimizationRecord createOptimizationRecord(
      String testClass, String level, String sourceLocation) {
    String title = "@DirtiesContext detected (" + level + ")";
    String description =
        String.format(
            "The test class '%s' uses @DirtiesContext which prevents Spring context caching. "
                + "This forces Spring to recreate the application context for each test, significantly "
                + "increasing test execution time.",
            testClass);

    String recommendation = buildRecommendation(level);

    return new OptimizationRecord(
        testClass,
        getTestSmellType(),
        title,
        description,
        recommendation,
        determineSeverity(level),
        sourceLocation);
  }

  private String buildRecommendation(String level) {
    if (level.startsWith("class-level")) {
      return "Consider the following alternatives:\n"
          + "• Remove @DirtiesContext and redesign tests to be more isolated\n"
          + "• Use @MockBean or @SpyBean instead of modifying application state\n"
          + "• Move context-modifying operations to separate test classes\n"
          + "• Use @Transactional with @Rollback for database operations\n"
          + "• Consider using TestContainers for integration tests";
    } else {
      return "Consider the following alternatives:\n"
          + "• Remove @DirtiesContext from individual methods\n"
          + "• Use @MockBean or @SpyBean for method-specific mocking\n"
          + "• Use @Transactional with @Rollback for database state changes\n"
          + "• Refactor to avoid modifying shared application state\n"
          + "• Group context-modifying tests into separate test classes";
    }
  }

  private OptimizationRecord.Severity determineSeverity(String level) {
    return level.startsWith("class-level")
        ? OptimizationRecord.Severity.HIGH
        : OptimizationRecord.Severity.MEDIUM;
  }
}
