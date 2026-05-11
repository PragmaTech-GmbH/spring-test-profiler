package digital.pragmatech.testing.plugins;

public final class SyntheticContextCustomizerPlugin implements ContextCustomizerPlugin {

  @Override
  public boolean supports(Object contextCustomizer) {
    return contextCustomizer instanceof SyntheticContextCustomizer;
  }

  @Override
  public String describe(Object contextCustomizer) {
    SyntheticContextCustomizer customizer = (SyntheticContextCustomizer) contextCustomizer;
    return "SyntheticContextCustomizer[name="
        + customizer.name()
        + ", baseUrlProperty="
        + customizer.baseUrlProperty()
        + "]";
  }
}
