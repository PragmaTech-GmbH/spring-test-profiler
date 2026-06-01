package digital.pragmatech.testing.extensions;

public final class SyntheticContextCustomizerExtension implements ContextCustomizerExtension {

  @Override
  public boolean supports(Object contextCustomizer) {
    return contextCustomizer instanceof SyntheticContextCustomizer;
  }

  @Override
  public String describe(Object contextCustomizer) {
    SyntheticContextCustomizer customizer = (SyntheticContextCustomizer) contextCustomizer;
    return "SyntheticContextCustomizer[identifier="
        + customizer.identifier()
        + ", configurationProperty="
        + customizer.configurationProperty()
        + "]";
  }
}
