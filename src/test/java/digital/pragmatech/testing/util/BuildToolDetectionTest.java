package digital.pragmatech.testing.util;

import java.util.Map;
import java.util.Properties;

import digital.pragmatech.testing.util.BuildToolDetection.BuildTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuildToolDetectionTest {

  @Test
  void shouldDetectMavenSurefireFork() {
    Properties properties = new Properties();
    properties.setProperty(
        "sun.java.command", "/app/target/surefire/surefirebooter-1.jar /app/target/surefire 2026");
    properties.setProperty("surefire.real.class.path", "/app/target/surefire/surefirebooter-1.jar");

    assertThat(BuildToolDetection.detect(properties, Map.of())).isEqualTo(BuildTool.MAVEN);
  }

  @Test
  void shouldDetectGradleTestWorker() {
    Properties properties = new Properties();
    properties.setProperty(
        "sun.java.command",
        "worker.org.gradle.process.internal.worker.GradleWorkerMain 'Gradle Test Executor 1'");

    assertThat(BuildToolDetection.detect(properties, Map.of())).isEqualTo(BuildTool.GRADLE);
  }

  @Test
  void shouldDetectBazelFromTestEnvironment() {
    assertThat(
            BuildToolDetection.detect(
                new Properties(), Map.of("TEST_SRCDIR", "/runfiles", "TEST_TMPDIR", "/tmp/test")))
        .isEqualTo(BuildTool.BAZEL);
  }

  @Test
  void shouldDetectFromOtherThreadThanTheBuildToolThread() throws InterruptedException {
    Properties properties = new Properties();
    properties.setProperty("surefire.test.class.path", "/app/target/test-classes");
    BuildTool[] result = new BuildTool[1];

    Thread shutdownHookLikeThread =
        new Thread(() -> result[0] = BuildToolDetection.detect(properties, Map.of()));
    shutdownHookLikeThread.start();
    shutdownHookLikeThread.join();

    assertThat(result[0]).isEqualTo(BuildTool.MAVEN);
  }

  @Test
  void shouldReturnUnknownWithoutAnySignal() throws InterruptedException {
    BuildTool[] result = new BuildTool[1];

    // A fresh thread has no build tool frames, so the stack trace fallback finds nothing
    Thread plainThread =
        new Thread(() -> result[0] = BuildToolDetection.detect(new Properties(), Map.of()));
    plainThread.start();
    plainThread.join();

    assertThat(result[0]).isEqualTo(BuildTool.UNKNOWN);
  }
}
