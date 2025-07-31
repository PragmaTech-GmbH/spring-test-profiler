package digital.pragmatech.testing.optimization;

import java.nio.file.Path;
import java.util.Set;

public interface StaticAnalysisContext {

  Set<String> getTestClasses();

  Path getProjectRoot();

  Set<Path> getTestSourcePaths();

  String getClassContent(String className);

  boolean hasAnnotation(String className, String annotationName);

  Set<String> getAnnotationsForClass(String className);

  Set<String> getMethodAnnotations(String className, String methodName);
}
