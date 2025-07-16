package digital.pragmatech.testing.util;

import java.util.Arrays;

public class BuildToolDetection {

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
}
