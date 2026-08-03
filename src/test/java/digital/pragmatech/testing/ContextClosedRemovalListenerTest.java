package digital.pragmatech.testing;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListenerAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.event.AfterTestClassEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for GitHub issue #58: the context-removal listener must declare a resolvable
 * ContextClosedEvent type so event multicasters never deliver unrelated events (e.g. Spring's
 * AfterTestClassEvent) to it. A lambda's generic event type cannot be resolved, making it match
 * every event type; Spring Modulith's PersistentApplicationEventMulticaster invokes listeners
 * without catching ClassCastException, so an unresolvable listener breaks test builds.
 */
class ContextClosedRemovalListenerTest {

  @Test
  void supportsOnlyContextClosedEvents() {
    var listener = new SpringTestProfilerListener.ContextClosedRemovalListener(null, null);
    var adapter = new GenericApplicationListenerAdapter(listener);

    assertTrue(
        adapter.supportsEventType(ResolvableType.forClass(ContextClosedEvent.class)),
        "Listener must handle ContextClosedEvent");
    assertFalse(
        adapter.supportsEventType(ResolvableType.forClass(AfterTestClassEvent.class)),
        "Listener must not be invoked for test events (issue #58)");
  }
}
