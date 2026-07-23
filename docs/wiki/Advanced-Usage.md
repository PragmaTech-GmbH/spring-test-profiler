# Advanced Usage

## Custom Context Customizer Descriptions

Spring Test Profiler can show richer context customizer details when your project exposes a `ContextCustomizerExtension` bean. This is useful when a customizer class is the same across test contexts, but its internal configuration is different. A common example is a WireMock `WireMockContextCustomizer`: two tests can both use the same customizer class, while each test configures different mock names, ports, files, or properties.

Without an extension, the report falls back to the customizer's simple class name - which makes two differing contexts look identical in the diff view.

### Implementing an Extension

Create a Spring bean in your test application context, for example with `@Component` or a test `@Bean` method:

```java
package com.example.testing;

import digital.pragmatech.testing.extensions.ContextCustomizerExtension;
import org.springframework.stereotype.Component;

@Component
class ExampleContextCustomizerExtension implements ContextCustomizerExtension {

  @Override
  public boolean supports(Object contextCustomizer) {
    // Return true only for the customizer type this extension knows how to describe.
    return contextCustomizer.getClass().getName().contains("ExampleContextCustomizer");
  }

  @Override
  public String describe(Object contextCustomizer) {
    // Return a stable, human-readable summary of the fields that make contexts differ.
    return contextCustomizer.getClass().getSimpleName() + "[configuration=custom]";
  }
}
```

### Guidelines

- The `supports(...)` method should be narrow: check the exact customizer class or a known interface.
- The `describe(...)` method should include only deterministic configuration values that help explain why Spring created a separate context. Avoid identity hashes, timestamps, random ports, or other values that change between runs unless they are the actual configuration you want to compare.
- If no extension supports a customizer, the report falls back to the customizer class simple name.

## Custom Report Directory

By default, reports land in `target/spring-test-profiler/` (Maven) or `build/spring-test-profiler/` (Gradle). Override the location with the `pragmatech.spring.test.insight.report.dir` system property:

```bash
./mvnw verify -Dpragmatech.spring.test.insight.report.dir=/tmp/profiler-reports
```

See the [Configuration Reference](Configuration-Reference.md) for details.

## JSON Report (Beta)

Next to the HTML report, the profiler can emit a structured JSON report for programmatic consumption - for example to track cache hit rates across CI builds or feed dashboards.

Enable it with:

```bash
./mvnw verify -Dspring.test.insight.json.beta=true
```

The JSON file is written to the same report directory as the HTML report.

> [!NOTE]
> The JSON format is in beta: its structure may change between releases without notice. Pin the profiler version if you build tooling on top of it, and share feedback via the [issue tracker](https://github.com/PragmaTech-GmbH/spring-test-profiler/issues).
