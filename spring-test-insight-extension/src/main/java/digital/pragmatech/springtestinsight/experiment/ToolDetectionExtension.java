package digital.pragmatech.springtestinsight.experiment;

import java.util.Arrays;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ToolDetectionExtension implements BeforeAllCallback {

  // Enum to represent the detected tool, now with VSCode and NetBeans
  public enum TriggeringTool {
    MAVEN,
    GRADLE,
    INTELLIJ,
    ECLIPSE,
    VSCODE,
    NETBEANS,
    UNKNOWN
  }

  private static TriggeringTool detectedTool;

  // We use a static block to ensure this runs only once per classloader
  static {
    detectedTool = detectTool();
    System.out.println("JUnit execution triggered by: " + detectedTool);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    context
      .getStore(ExtensionContext.Namespace.GLOBAL)
      .put("execution.tool", detectedTool);
  }

  public static TriggeringTool getDetectedTool() {
    return detectedTool;
  }

  private static TriggeringTool detectTool() {
    try {
      throw new RuntimeException("Tool Detection");
    }
    catch (RuntimeException e) {
      String stackTrace = Arrays.toString(e.getStackTrace());

      if (stackTrace.contains("com.intellij.rt.junit")) {
        return TriggeringTool.INTELLIJ;
      }
      if (stackTrace.contains("org.eclipse.jdt.internal.junit")) {
        return TriggeringTool.ECLIPSE;
      }

      if (stackTrace.contains("com.microsoft.java.test.runner")) {
        return TriggeringTool.VSCODE;
      }

      if (stackTrace.contains("org.netbeans.modules")) {
        return TriggeringTool.NETBEANS;
      }

      if (stackTrace.contains("org.apache.maven.surefire")) {
        return TriggeringTool.MAVEN;
      }

      if (stackTrace.contains("org.gradle.api.internal.tasks.testing")) {
        return TriggeringTool.GRADLE;
      }

      return TriggeringTool.UNKNOWN;
    }
  }
}
