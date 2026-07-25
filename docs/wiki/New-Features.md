# New Features

Curated highlights of what landed in recent releases. For the complete changelog, see the [GitHub releases](https://github.com/PragmaTech-GmbH/spring-test-profiler/releases).

## 0.2.1

### Flat JSON Summary Report with CI Verification ([#53](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/53))

Every run now writes a machine-readable `results.json` next to the HTML report - a single flat object with metrics like `contextsCreated`, `contextCacheHitRatio`, and `totalContextCreationTimeMs`. Pin your expected context count in CI and fail the build when it regresses:

```bash
jq '.contextsCreated' target/spring-test-profiler/results.json
```

This replaces the previous beta JSON report and its `spring.test.insight.json.beta` flag. See [Advanced Usage](Advanced-Usage.md#json-summary-report) for the full metric reference and ready-to-use CI snippets.

### Context Caching Timeline Visualization ([#34](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/34), [#50](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/50))

The report now renders a first visualization of the context cache over time, including tracking of context removals from the cache.

### Context Caching Explained with an Animation ([#49](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/49))

The report's theory section explains Spring's context caching mechanism with an animation: configuration scan, `MergedContextConfiguration` cache key, hit or miss. The same animation is embedded in [Spring Context Caching](Spring-Context-Caching.md).

### Total Contexts Created Tile ([#54](https://github.com/PragmaTech-GmbH/spring-test-profiler/pull/54))

The summary section gained a "Total Contexts Created" tile, surfacing the most important optimization number at first glance.

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
