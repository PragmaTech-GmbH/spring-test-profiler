package digital.pragmatech.testing.extensions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCustomizerExtensionRegistryTest {

  @AfterEach
  void clearExtensionRegistry() {
    ContextCustomizerExtensionRegistry.clear();
  }

  @Test
  void shouldRegisterExtensionsFromSpringBeans() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.registerBean(SyntheticContextCustomizerExtension.class);
      context.refresh();

      ContextCustomizerExtensionRegistry.registerAll(
          context.getBeansOfType(ContextCustomizerExtension.class).values());

      assertThat(ContextCustomizerExtensionRegistry.getExtensions())
          .hasOnlyElementsOfType(SyntheticContextCustomizerExtension.class);
    }
  }
}
