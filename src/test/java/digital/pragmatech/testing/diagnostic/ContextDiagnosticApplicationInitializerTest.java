package digital.pragmatech.testing.diagnostic;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.assertj.core.api.Assertions.assertThat;

class ContextDiagnosticApplicationInitializerTest {

  @Test
  void shouldRegisterBeanInParentContextOnly() {
    // Create parent context
    var parentContext = new AnnotationConfigApplicationContext();

    var initializer = new ContextDiagnosticApplicationInitializer();
    initializer.initialize(parentContext);

    // Refresh parent context - should register bean
    parentContext.refresh();

    assertThat(parentContext.containsBean("contextDiagnostic")).isTrue();

    // Create child context
    AnnotationConfigApplicationContext childContext = new AnnotationConfigApplicationContext();
    childContext.setParent(parentContext);

    initializer.initialize(childContext);
    childContext.refresh();

    // Child should not have the bean locally (parent still has it)
    assertThat(childContext.containsLocalBean("contextDiagnostic")).isFalse();
    assertThat(parentContext.containsBean("contextDiagnostic")).isTrue();

    parentContext.close();
    childContext.close();
  }

  @Test
  void shouldNotRegisterBeanTwiceOnMultipleRefresh() {
    var context = new AnnotationConfigApplicationContext();
    var initializer = new ContextDiagnosticApplicationInitializer();

    initializer.initialize(context);

    context.refresh();

    // Simulate additional refresh events
    context.publishEvent(new ContextRefreshedEvent(context));
    context.publishEvent(new ContextRefreshedEvent(context));

    // Should still have only one bean
    assertThat(context.getBeansOfType(ContextDiagnostic.class)).hasSize(1);

    context.close();
  }

  @Test
  void shouldHaveCorrectEqualsAndHashCode() {
    var initializer1 = new ContextDiagnosticApplicationInitializer();
    var initializer2 = new ContextDiagnosticApplicationInitializer();

    assertThat(initializer1).isEqualTo(initializer2);
    assertThat(initializer1.hashCode()).isEqualTo(initializer2.hashCode());
  }

  @Test
  void shouldNotThrowExceptionWithHierarchicalContexts() {
    // This test verifies the fix doesn't break with real hierarchical contexts
    try (var parentContext = new AnnotationConfigApplicationContext();
        var childContext = new AnnotationConfigApplicationContext()) {
      childContext.setParent(parentContext);

      var initializer = new ContextDiagnosticApplicationInitializer();
      initializer.initialize(parentContext);
      initializer.initialize(childContext);

      parentContext.refresh();
      childContext.refresh();

      assertThat(parentContext.isActive()).isTrue();
      assertThat(childContext.isActive()).isTrue();
    }
  }
}
