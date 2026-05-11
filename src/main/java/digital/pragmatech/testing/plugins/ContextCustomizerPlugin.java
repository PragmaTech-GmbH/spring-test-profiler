package digital.pragmatech.testing.plugins;

/**
 * Extension point for describing Spring test context customizers in reports.
 *
 * <p>Projects can register implementations with Java's {@link java.util.ServiceLoader} under {@code
 * META-INF/services/digital.pragmatech.testing.plugins.ContextCustomizerPlugin}.
 */
public interface ContextCustomizerPlugin {

  /** Returns {@code true} when this plugin can describe the supplied context customizer. */
  boolean supports(Object contextCustomizer);

  /** Returns a stable, human-readable description of the supplied context customizer. */
  String describe(Object contextCustomizer);
}
