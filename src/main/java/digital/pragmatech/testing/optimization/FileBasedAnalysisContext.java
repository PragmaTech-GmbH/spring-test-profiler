package digital.pragmatech.testing.optimization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedAnalysisContext implements StaticAnalysisContext {

  private static final Logger logger = LoggerFactory.getLogger(FileBasedAnalysisContext.class);

  private final Path projectRoot;
  private final Set<String> testClasses;
  private final Set<Path> testSourcePaths;

  public FileBasedAnalysisContext(Set<String> testClasses) {
    this.testClasses = new HashSet<>(testClasses);
    this.projectRoot = detectProjectRoot();
    this.testSourcePaths = findTestSourcePaths();
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
    return new HashSet<>(testSourcePaths);
  }

  @Override
  public String getClassContent(String className) {
    Path classFile = findClassFile(className);
    if (classFile != null && Files.exists(classFile)) {
      try {
        return Files.readString(classFile);
      } catch (IOException e) {
        logger.warn("Failed to read class file {}: {}", classFile, e.getMessage());
      }
    }
    return "";
  }

  @Override
  public boolean hasAnnotation(String className, String annotationName) {
    return hasClassLevelAnnotation(className, annotationName);
  }

  public boolean hasClassLevelAnnotation(String className, String annotationName) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return false;
    }

    // Get simple annotation name
    String simpleAnnotationName =
        annotationName.contains(".")
            ? annotationName.substring(annotationName.lastIndexOf('.') + 1)
            : annotationName;

    // Look for class-level annotations only - annotations that appear before the class declaration
    // First find the class declaration
    String simpleClassName = extractSimpleClassName(className);
    Pattern classPattern =
        Pattern.compile(
            "^\\s*(public\\s+)?(abstract\\s+|final\\s+)?class\\s+"
                + Pattern.quote(simpleClassName)
                + "\\b",
            Pattern.MULTILINE);

    Matcher classMatcher = classPattern.matcher(content);
    if (!classMatcher.find()) {
      return false;
    }

    int classStartPosition = classMatcher.start();
    String beforeClassContent = content.substring(0, classStartPosition);

    // Look for the annotation before the class declaration only
    Pattern annotationPattern =
        Pattern.compile("^\\s*@" + Pattern.quote(simpleAnnotationName) + "\\b", Pattern.MULTILINE);
    return annotationPattern.matcher(beforeClassContent).find();
  }

  public boolean hasMethodLevelAnnotation(String className, String annotationName) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return false;
    }

    // Get simple annotation name
    String simpleAnnotationName =
        annotationName.contains(".")
            ? annotationName.substring(annotationName.lastIndexOf('.') + 1)
            : annotationName;

    // Look for method-level annotations - annotations that appear before method declarations
    // but not before the class declaration
    String simpleClassName = extractSimpleClassName(className);
    Pattern classPattern =
        Pattern.compile(
            "^\\s*(public\\s+)?(abstract\\s+|final\\s+)?class\\s+"
                + Pattern.quote(simpleClassName)
                + "\\b",
            Pattern.MULTILINE);

    Matcher classMatcher = classPattern.matcher(content);
    if (!classMatcher.find()) {
      return false;
    }

    int classEndPosition = classMatcher.end();
    String afterClassContent = content.substring(classEndPosition);

    // Look for the annotation in the class body (after class declaration)
    Pattern annotationPattern =
        Pattern.compile("^\\s*@" + Pattern.quote(simpleAnnotationName) + "\\b", Pattern.MULTILINE);
    return annotationPattern.matcher(afterClassContent).find();
  }

  private String extractSimpleClassName(String fullyQualifiedClassName) {
    return fullyQualifiedClassName.contains(".")
        ? fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf('.') + 1)
        : fullyQualifiedClassName;
  }

  @Override
  public Set<String> getAnnotationsForClass(String className) {
    String content = getClassContent(className);
    if (content.isEmpty()) {
      return Set.of();
    }

    Set<String> annotations = new HashSet<>();
    // Pattern to match annotations like @SomeName or @package.SomeName
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
    // Find method and extract annotations before it
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

  private Path detectProjectRoot() {
    Path currentDir = Paths.get(System.getProperty("user.dir"));

    // Look for common project indicators
    while (currentDir != null) {
      if (Files.exists(currentDir.resolve("pom.xml"))
          || Files.exists(currentDir.resolve("build.gradle"))
          || Files.exists(currentDir.resolve("build.gradle.kts"))) {
        return currentDir;
      }
      currentDir = currentDir.getParent();
    }

    return Paths.get(System.getProperty("user.dir"));
  }

  private Set<Path> findTestSourcePaths() {
    Set<Path> paths = new HashSet<>();

    // Common Maven structure
    Path mavenTest = projectRoot.resolve("src/test/java");
    if (Files.exists(mavenTest)) {
      paths.add(mavenTest);
    }

    // Common Gradle structure
    Path gradleTest = projectRoot.resolve("src/test/java");
    if (Files.exists(gradleTest)) {
      paths.add(gradleTest);
    }

    // Additional test source directories can be added here

    return paths;
  }

  private Path findClassFile(String className) {
    String relativePath = className.replace('.', '/') + ".java";

    for (Path testSourcePath : testSourcePaths) {
      Path classFile = testSourcePath.resolve(relativePath);
      if (Files.exists(classFile)) {
        return classFile;
      }
    }

    return null;
  }
}
