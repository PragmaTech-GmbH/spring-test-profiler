# New Features

Curated highlights of what landed in recent releases. For the complete changelog, see the [GitHub releases](https://github.com/PragmaTech-GmbH/spring-test-profiler/releases).

## 0.1.2

### Self-Contained Reports ([#46](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/46))

All CSS and JavaScript is now inlined into the HTML report. A report is a single file with no external assets, so it renders correctly when archived as a CI artifact, attached to an issue, or sent to a colleague.

## 0.1.1

### Context Customizer Extensions in Reports ([#44](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/44))

The report can now display meaningful descriptions for context customizers whose class name alone does not explain why two contexts differ (for example WireMock customizers). Provide a `ContextCustomizerExtension` bean and the diff view shows your custom description instead of just the class name.

```java
@Component
class WireMockCustomizerExtension implements ContextCustomizerExtension {
  @Override
  public boolean supports(Object contextCustomizer) {
    return contextCustomizer.getClass().getName().contains("WireMockContextCustomizer");
  }

  @Override
  public String describe(Object contextCustomizer) {
    return "WireMock[...stable configuration summary...]";
  }
}
```

See the full guide in [Advanced Usage](Advanced-Usage.md#custom-context-customizer-descriptions).

## 0.1.0

### Reworked Report Overview ([#42](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/42))

The core tiles at the top of the report were adjusted to surface the most decision-relevant numbers first.

## 0.0.18

### Spring Framework 5 / Spring Boot 2 Compatibility ([#40](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/40))

The profiler now also runs on Spring Framework 5 (Spring Boot 2.x), extending support across three major Spring generations.

### Sortable Context Cache Entries ([#39](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/39))

The Context Cache Entries section gained sorting controls, so you can order contexts by load time and find the most expensive ones first.

### Annotation-Based Context Filtering ([#36](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/36))

Filter contexts in the report by the test annotations that produced them (for example `@SpringBootTest` vs `@WebMvcTest`).

## 0.0.16

### Spring Boot 4 Support ([#33](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/33))

The profiler supports Spring Framework 7 / Spring Boot 4, alongside a simplified report.

---

Older changes are listed on the [releases page](https://github.com/PragmaTech-GmbH/spring-test-profiler/releases).
