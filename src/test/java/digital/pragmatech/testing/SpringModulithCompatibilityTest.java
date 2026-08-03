package digital.pragmatech.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.modulith.events.support.PersistentApplicationEventMulticaster;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Regression test for GitHub issue #58: with Spring Modulith's JDBC event publication registry
 * active, the context's event multicaster is Modulith's PersistentApplicationEventMulticaster,
 * which invokes listeners WITHOUT the ClassCastException safety net of Spring's default
 * multicaster. The test framework publishes test events (PrepareTestInstanceEvent,
 * AfterTestClassEvent, ...) into this context because the profiler registers context listeners; if
 * any profiler listener has an unresolvable event type (e.g. a mistyped lambda), this test class
 * errors with "AfterTestClassEvent cannot be cast to ContextClosedEvent" in afterTestClass.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringModulithCompatibilityTest.ModulithConfig.class)
class SpringModulithCompatibilityTest {

  @Autowired private ApplicationContext applicationContext;

  @Test
  void modulithEventMulticasterIsActive() {
    // Guard: the scenario is only covered while Modulith replaces the default multicaster
    assertInstanceOf(
        PersistentApplicationEventMulticaster.class,
        applicationContext.getBean(
            AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class ModulithConfig {}
}
