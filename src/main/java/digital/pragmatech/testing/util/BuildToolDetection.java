package digital.pragmatech.testing.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * Detects the build tool that started the current test JVM.
 *
 * <p>The detection only looks at JVM-wide state (system properties, the JVM launch command and
 * environment variables). It does not depend on the calling thread, so it also works from the
 * shutdown hook that generates the report.
 */
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
    detectedBuildTool = detect(System.getProperties(), System.getenv());
  }

  public static BuildTool getDetectedBuildTool() {
    return detectedBuildTool;
  }

  static BuildTool detect(Properties systemProperties, Map<String, String> environment) {
    String launchCommand = systemProperties.getProperty("sun.java.command", "");
    String classPath = systemProperties.getProperty("java.class.path", "");

    // Gradle test workers are started via GradleWorkerMain ("Gradle Test Executor N")
    if (launchCommand.contains("GradleWorkerMain")
        || launchCommand.contains("Gradle Test Executor")
        || classPath.contains("gradle-worker.jar")) {
      return BuildTool.GRADLE;
    }

    // Surefire and Failsafe forked JVMs set these properties and start via the surefire booter
    if (systemProperties.containsKey("surefire.real.class.path")
        || systemProperties.containsKey("surefire.test.class.path")
        || launchCommand.contains("surefire")
        || launchCommand.contains("org.apache.maven")
        || systemProperties.containsKey("maven.home")) {
      return BuildTool.MAVEN;
    }

    // Bazel exposes the test sandbox via environment variables
    if (environment.containsKey("TEST_SRCDIR") && environment.containsKey("TEST_TMPDIR")) {
      return BuildTool.BAZEL;
    }

    if (launchCommand.contains("org.apache.tools.ant")
        || systemProperties.containsKey("ant.home")) {
      return BuildTool.ANT;
    }

    return detectFromStackTrace();
  }

  // Last resort: works only when the class is first used on a thread started by the build tool
  private static BuildTool detectFromStackTrace() {
    String stackTrace = Arrays.toString(new Throwable().getStackTrace());

    if (stackTrace.contains("org.apache.maven")) {
      return BuildTool.MAVEN;
    }

    if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
      return BuildTool.GRADLE;
    }

    return BuildTool.UNKNOWN;
  }
}
