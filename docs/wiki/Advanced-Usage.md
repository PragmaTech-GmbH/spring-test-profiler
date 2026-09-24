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

## JSON Summary Report

Next to the HTML report, a flat JSON summary is written for machine consumption (CI checks, dashboards, trend tracking):

- Maven: `target/spring-test-profiler/results.json` (latest run) plus a timestamped `test-profiler-report-<timestamp>.json` per run
- Gradle: `build/spring-test-profiler/results.json` plus the timestamped file per run

The JSON contains a single flat object, so it can be consumed with simple tooling:

```bash
jq '.contextsCreated' target/spring-test-profiler/results.json
```

### Available Metrics

All duration values are in milliseconds. The `schemaVersion` field allows the format to evolve without breaking consumers (currently `1`). Test-level counts (classes, methods, pass/fail) are intentionally not included - build tools like Surefire, Failsafe, and Gradle already report them.

| Field | Description |
|---|---|
| `schemaVersion` | Version of the JSON format |
| `profilerVersion` | Spring Test Profiler version that produced the file |
| `generatedAt` | Timestamp of report generation |
| `buildTool` | Detected build tool (Maven/Gradle) |
| `totalDurationMs` | Total duration of the profiled test run |
| `contextsCreated` | Number of application contexts created |
| `contextCacheHits` / `contextCacheMisses` | Context cache hit and miss counts |
| `contextCacheHitRatio` | Cache hit ratio |
| `springContextCacheSize` / `springContextCacheMaxSize` | Spring's internal cache usage and limit |
| `totalContextCreationTimeMs` | Total time spent creating contexts |
| `wastedContextTimeMs` | Time spent creating contexts that could have been reused |
| `potentialTimeSavingsMs` / `potentialTimeSavingsPercent` | Estimated savings from full context reuse |
| `availableProcessors` | Processor count of the machine running the tests |

### Guard Your Context Count in CI

Once you have optimized your test suite, you can pin the expected number of created contexts and fail the build when it regresses (for example when someone introduces a new `@DirtiesContext` or an accidental context configuration difference). Run this after your test suite, e.g. as a CI step:

```bash
expectedContexts=3
actualContexts=$(jq -r '.contextsCreated' target/spring-test-profiler/results.json)

if [ "$actualContexts" != "$expectedContexts" ]; then
  echo "Expected $expectedContexts Spring contexts but $actualContexts were created"
  exit 1
fi
```

Other metrics work the same way, for example alerting when context creation time exceeds a budget:

```bash
totalContextCreationTimeMs=$(jq -r '.totalContextCreationTimeMs' target/spring-test-profiler/results.json)

if [ "$totalContextCreationTimeMs" -gt 60000 ]; then
  echo "Context creation took ${totalContextCreationTimeMs}ms, exceeding the 60s budget"
  exit 1
fi
```

The profiler repository uses the same approach for its own demo projects: each demo pins its expected context count in a `context-info.json` file, and the CI pipeline verifies the generated `results.json` against it with [`.github/scripts/verify-profiler-json.sh`](https://github.com/PragmaTech-GmbH/spring-test-profiler/blob/main/.github/scripts/verify-profiler-json.sh) - see [Demo Projects](Demo-Projects.md).
