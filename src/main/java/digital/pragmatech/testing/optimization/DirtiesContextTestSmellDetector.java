package digital.pragmatech.testing.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    // Check for class-level annotation using reflection to call the right methods
    boolean hasClassLevel = false;
    boolean hasMethodLevel = false;

    try {
      // Try to call hasClassLevelAnnotation if it exists
      var classLevelMethod =
          context.getClass().getMethod("hasClassLevelAnnotation", String.class, String.class);
      hasClassLevel =
          (Boolean) classLevelMethod.invoke(context, testClass, ANNOTATION_NAME)
              || (Boolean) classLevelMethod.invoke(context, testClass, FULL_ANNOTATION_NAME);

      // Try to call hasMethodLevelAnnotation if it exists
      var methodLevelMethod =
          context.getClass().getMethod("hasMethodLevelAnnotation", String.class, String.class);
      hasMethodLevel =
          (Boolean) methodLevelMethod.invoke(context, testClass, ANNOTATION_NAME)
              || (Boolean) methodLevelMethod.invoke(context, testClass, FULL_ANNOTATION_NAME);

    } catch (Exception e) {
      // Fallback to original logic for contexts that don't support class/method distinction
      if (context.hasAnnotation(testClass, ANNOTATION_NAME)
          || context.hasAnnotation(testClass, FULL_ANNOTATION_NAME)) {
        String sourceLocation = testClass + ":class-level";
        optimizations.add(createOptimizationRecord(testClass, "class-level", sourceLocation));
      }

      List<String> methodsWithDirtiesContext =
          findMethodsWithDirtiesContextUsingAnnotations(testClass, context);

      for (String methodName : methodsWithDirtiesContext) {
        String sourceLocation = testClass + ":" + methodName;
        optimizations.add(
            createOptimizationRecord(testClass, "method-level on " + methodName, sourceLocation));
      }
      return;
    }

    // Handle class-level annotations
    if (hasClassLevel) {
      String sourceLocation = testClass + ":class-level";
      optimizations.add(createOptimizationRecord(testClass, "class-level", sourceLocation));
    }

    // Handle method-level annotations
    if (hasMethodLevel) {
      List<String> methodsWithDirtiesContext =
          findMethodsWithDirtiesContextUsingAnnotations(testClass, context);

      for (String methodName : methodsWithDirtiesContext) {
        String sourceLocation = testClass + ":" + methodName;
        optimizations.add(
            createOptimizationRecord(testClass, "method-level on " + methodName, sourceLocation));
      }
    }
  }

  private List<String> findMethodsWithDirtiesContextUsingAnnotations(
      String testClass, StaticAnalysisContext context) {
    List<String> methods = new ArrayList<>();

    // Since we don't have direct access to method names from the context interface,
    // we need to parse the class content to find method names, then check their annotations
    String classContent = context.getClassContent(testClass);
    if (classContent.isEmpty()) {
      return methods;
    }

    // Extract method names from the class content
    List<String> methodNames = extractMethodNames(classContent);

    // Check each method for @DirtiesContext annotation
    for (String methodName : methodNames) {
      Set<String> methodAnnotations = context.getMethodAnnotations(testClass, methodName);
      if (methodAnnotations.contains(ANNOTATION_NAME)
          || methodAnnotations.contains(FULL_ANNOTATION_NAME)) {
        methods.add(methodName);
      }
    }

    return methods;
  }

  private List<String> extractMethodNames(String classContent) {
    List<String> methodNames = new ArrayList<>();

    // Pattern to find method declarations (public, private, protected methods with parameters)
    Pattern methodPattern =
        Pattern.compile(
            "(?:public|private|protected)\\s+(?:static\\s+)?(?:final\\s+)?(?:void|\\w+(?:<[^>]*>)?)\\s+(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[^{]*)?\\s*\\{",
            Pattern.MULTILINE | Pattern.DOTALL);

    Matcher matcher = methodPattern.matcher(classContent);
    while (matcher.find()) {
      String methodName = matcher.group(1);
      if (methodName != null && !methodName.isEmpty() && !methodName.equals("class")) {
        methodNames.add(methodName);
      }
    }

    return methodNames;
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

    String recommendation = buildRecommendation();

    return new OptimizationRecord(
        testClass,
        getTestSmellType(),
        title,
        description,
        recommendation,
        determineSeverity(level),
        sourceLocation);
  }

  private String buildRecommendation() {
    return """
        Consider the following alternatives:
        • Remove @DirtiesContext and redesign tests to be more isolated
        • Use @MockitoBean instead of modifying application state
        • Move context-modifying operations to separate test classes
        • Use @Transactional with @Rollback for database operations
      """;
  }

  private OptimizationRecord.Severity determineSeverity(String level) {
    if (level.contains("method-level")) {
      return OptimizationRecord.Severity.MEDIUM;
    } else {
      return OptimizationRecord.Severity.HIGH;
    }
  }
}
