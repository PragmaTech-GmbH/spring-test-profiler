package digital.pragmatech.testing;

import java.util.Arrays;

public class BuildToolDetection {

  // Enum to represent the detected tool, now with VSCode and NetBeans
  public enum ExecutionEnvironment {
    MAVEN,
    MAVEN_SUREFIRE,
    MAVEN_FAILSAFE,
    GRADLE,
    GRADLE_TEST,
    GRADLE_INTEGRATION_TEST,
    INTELLIJ,
    ECLIPSE,
    VSCODE,
    NETBEANS,
    UNKNOWN
  }

  private static ExecutionEnvironment detectedTool;

  // We use a static block to ensure this runs only once per classloader
  static {
    detectedTool = detectTool();
    System.out.println("JUnit execution triggered by: " + detectedTool);
  }

  public static ExecutionEnvironment getDetectedTool() {
    return detectedTool;
  }

  private static ExecutionEnvironment detectTool() {
    try {
      throw new RuntimeException("Tool Detection");
    }
    catch (RuntimeException e) {
      String stackTrace = Arrays.toString(e.getStackTrace());

      if (stackTrace.contains("com.intellij.rt.junit")) {
        return ExecutionEnvironment.INTELLIJ;
      }
      if (stackTrace.contains("org.eclipse.jdt.internal.junit")) {
        return ExecutionEnvironment.ECLIPSE;
      }

      if (stackTrace.contains("com.microsoft.java.test.runner")) {
        return ExecutionEnvironment.VSCODE;
      }

      if (stackTrace.contains("org.netbeans.modules")) {
        return ExecutionEnvironment.NETBEANS;
      }

      if (stackTrace.contains("org.apache.maven")) {
        return ExecutionEnvironment.MAVEN;
      }

      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        return ExecutionEnvironment.GRADLE;
      }

      return ExecutionEnvironment.UNKNOWN;
    }
  }

  public static ExecutionEnvironment detectBuildToolPhase(Class<?> testClass) {
    // First, detect the build tool
    ExecutionEnvironment buildTool = detectTool();

    // Then, detect the test phase based on naming conventions
    String className = testClass.getSimpleName();
    boolean isIntegrationTest = className.endsWith("IT") ||
      className.endsWith("IntegrationTest") ||
      className.contains("Integration");

    if (ExecutionEnvironment.MAVEN.equals(buildTool)) {
      return isIntegrationTest ? ExecutionEnvironment.MAVEN_FAILSAFE : ExecutionEnvironment.MAVEN_SUREFIRE;
    }
    else if (ExecutionEnvironment.GRADLE.equals(buildTool)) {
      return isIntegrationTest ? ExecutionEnvironment.GRADLE_INTEGRATION_TEST : ExecutionEnvironment.GRADLE_TEST;
    }
    else {
      return buildTool;
    }
  }

}
