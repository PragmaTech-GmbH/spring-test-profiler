package digital.pragmatech.testing.util;

import java.util.Arrays;

public class BuildToolDetection {

  public enum ExecutionEnvironment {
    MAVEN_SUREFIRE,
    MAVEN_FAILSAFE,
    GRADLE_TEST,
    GRADLE_INTEGRATION_TEST,
    INTELLIJ,
    ECLIPSE,
    VSCODE,
    NETBEANS,
    UNKNOWN
  }

  public enum BuildTool {
    MAVEN,
    GRADLE,
    BAZEL,
    ANT,
    UNKNOWN
  }

  private static final BuildTool detectedBuildTool;

  // We use a static block to ensure this runs only once per classloader
  static {
    detectedBuildTool = detectBuildTool();
  }

  public static BuildTool getDetectedBuildTool() {
    return detectedBuildTool;
  }

  private static BuildTool detectBuildTool() {
    try {
      throw new RuntimeException("Build Tool Detection");
    }
    catch (RuntimeException e) {
      String stackTrace = Arrays.toString(e.getStackTrace());

      if (stackTrace.contains("org.apache.maven")) {
        return BuildTool.MAVEN;
      }

      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        return BuildTool.GRADLE;
      }

      return BuildTool.UNKNOWN;
    }
  }

  public static ExecutionEnvironment detectExecutionEnvironment(String className) {
    try {
      throw new RuntimeException("Execution Environment Detection");
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
        if (isIntegrationTest(className)) {
          return ExecutionEnvironment.MAVEN_FAILSAFE;
        }
        else {
          return ExecutionEnvironment.MAVEN_SUREFIRE;
        }
      }

      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        if (isIntegrationTest(className)) {
          return ExecutionEnvironment.GRADLE_TEST;
        }
        else {
          return ExecutionEnvironment.GRADLE_INTEGRATION_TEST;
        }
      }

      return ExecutionEnvironment.UNKNOWN;
    }
  }

  private static boolean isIntegrationTest(String className) {
    return className.contains("IT") || className.contains("IntegrationTest");
  }
}
