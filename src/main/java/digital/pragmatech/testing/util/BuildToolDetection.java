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

  private static final ExecutionEnvironment detectedExecutionEnvironment;
  private static final BuildTool detectedBuildTool;

  // We use a static block to ensure this runs only once per classloader
  static {
    detectedExecutionEnvironment = detectExecutionEnvironment();
    detectedBuildTool = detectBuildTool();
    System.out.println("JUnit execution triggered by: " + detectedExecutionEnvironment);
  }

  public static BuildTool getDetectedBuildTool() {
    return detectedBuildTool;
  }

  public static ExecutionEnvironment getDetectedExecutionEnvironment() {
    return detectedExecutionEnvironment;
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


  private static ExecutionEnvironment detectExecutionEnvironment() {
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

      if (stackTrace.contains("failsafe")) {
        return ExecutionEnvironment.MAVEN_FAILSAFE;
      }

      if (stackTrace.contains("surefire")) {
        return ExecutionEnvironment.MAVEN_SUREFIRE;
      }

      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        return ExecutionEnvironment.GRADLE_TEST;
      }

      return ExecutionEnvironment.UNKNOWN;
    }
  }
}
