package digital.pragmatech.testing.extensions;

/**
 * Extension point for describing Spring test context customizers in reports.
 *
 * <p>Projects can provide implementations as Spring beans, for example through {@code @Component}
 * scanning or a test {@code @Bean} method.
 */
public interface ContextCustomizerExtension {

  /** Returns {@code true} when this extension can describe the supplied context customizer. */
  boolean supports(Object contextCustomizer);

  /** Returns a stable, human-readable description of the supplied context customizer. */
  String describe(Object contextCustomizer);
}
