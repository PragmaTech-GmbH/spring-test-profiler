package digital.pragmatech.testing.sample;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the claim in the README: an exception thrown from a {@link LauncherSessionListener} after
 * the last test fails the run, so it can be used as a "run last" budget check.
 */
class ContextBudgetSessionListenerTest {

  @Test
  void shouldPropagateExceptionFromSessionClosed() {
    LauncherConfig config =
        LauncherConfig.builder()
            .enableLauncherSessionListenerAutoRegistration(false)
            .addLauncherSessionListeners(
                new LauncherSessionListener() {
                  @Override
                  public void launcherSessionClosed(LauncherSession session) {
                    throw new AssertionError("budget exceeded");
                  }
                })
            .build();

    assertThrows(
        AssertionError.class,
        () -> {
          try (LauncherSession session = LauncherFactory.openSession(config)) {
            // no tests, only the close hook matters
          }
        });
  }
}
