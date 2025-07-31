package digital.pragmatech.testing.optimization;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestStaticAnalysisContext implements StaticAnalysisContext {

  private final Set<String> testClasses = new HashSet<>();
  private final Map<String, String> classContents = new HashMap<>();
  private final Path projectRoot = Paths.get(System.getProperty("user.dir"));

  public void addTestClass(String className) {
    testClasses.add(className);
  }

  public void setClassContent(String className, String content) {
    classContents.put(className, content);
  }

  @Override
  public Set<String> getTestClasses() {
    return new HashSet<>(testClasses);
  }

  @Override
  public Path getProjectRoot() {
    return projectRoot;
  }

  @Override
  public Set<Path> getTestSourcePaths() {
    return Set.of(projectRoot.resolve("src/test/java"));
  }

  @Override
  public String getClassContent(String className) {
    return classContents.getOrDefault(className, "");
  }

  @Override
  public boolean hasAnnotation(String className, String annotationName) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return false;
    }

    String simpleAnnotationName =
        annotationName.contains(".")
            ? annotationName.substring(annotationName.lastIndexOf('.') + 1)
            : annotationName;

    Pattern pattern = Pattern.compile("@" + Pattern.quote(simpleAnnotationName) + "\\b");
    return pattern.matcher(content).find();
  }

  @Override
  public Set<String> getAnnotationsForClass(String className) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return Set.of();
    }

    Set<String> annotations = new HashSet<>();
    Pattern pattern =
        Pattern.compile("@([a-zA-Z_$][a-zA-Z0-9_$.]*(?:\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*)");
    Matcher matcher = pattern.matcher(content);

    while (matcher.find()) {
      annotations.add(matcher.group(1));
    }

    return annotations;
  }

  @Override
  public Set<String> getMethodAnnotations(String className, String methodName) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return Set.of();
    }

    Set<String> annotations = new HashSet<>();
    String methodPattern =
        "(?:@[^\\n]*\\s+)*\\s*(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*\\w+\\s+"
            + Pattern.quote(methodName)
            + "\\s*\\(";
    Pattern pattern = Pattern.compile(methodPattern, Pattern.MULTILINE | Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);

    if (matcher.find()) {
      String methodDeclaration = matcher.group();
      Pattern annotationPattern =
          Pattern.compile("@([a-zA-Z_$][a-zA-Z0-9_$.]*(?:\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*)");
      Matcher annotationMatcher = annotationPattern.matcher(methodDeclaration);

      while (annotationMatcher.find()) {
        annotations.add(annotationMatcher.group(1));
      }
    }

    return annotations;
  }
}
