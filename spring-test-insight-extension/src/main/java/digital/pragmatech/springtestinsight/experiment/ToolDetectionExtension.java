package digital.pragmatech.springtestinsight.experiment;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

public class ToolDetectionExtension implements BeforeAllCallback {

  // Enum to represent the detected tool
  public enum TriggeringTool {
    MAVEN,
    GRADLE,
    INTELLIJ,
    ECLIPSE,
    UNKNOWN
  }

  private static TriggeringTool detectedTool;

  // We use a static block to ensure this runs only once
  static {
    detectedTool = detectTool();
    System.out.println("JUnit execution triggered by: " + detectedTool);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // The detection is already done, but you could add logic here
    // to store the tool in the context's store if needed for other extensions.
    context
      .getStore(ExtensionContext.Namespace.GLOBAL)
      .put("execution.tool", detectedTool);
  }

  public static TriggeringTool getDetectedTool() {
    return detectedTool;
  }

  /**
   * Inspects the stack trace to find markers of common build tools and IDEs.
   */
  private static TriggeringTool detectTool() {
    try {
      throw new RuntimeException("Tool Detection");
    } catch (RuntimeException e) {
      String stackTrace = Arrays.toString(e.getStackTrace());

      if (stackTrace.contains("org.apache.maven.surefire")) {
        return TriggeringTool.MAVEN;
      }
      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        return TriggeringTool.GRADLE;
      }
      if (stackTrace.contains("com.intellij.rt.junit")) {
        return TriggeringTool.INTELLIJ;
      }
      if (stackTrace.contains("org.eclipse.jdt.internal.junit")) {
        return TriggeringTool.ECLIPSE;
      }
      return TriggeringTool.UNKNOWN;
    }
  }
}
