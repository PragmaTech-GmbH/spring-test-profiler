# Newsletter Update: Context Cache Timeline + results.json

- Purpose: newsletter section announcing the two latest Spring Test Profiler features (Context Cache Timeline, results.json CI guard) with a short tool intro
- Embeds (GitHub raw URLs, valid once the assets are on `main`):
  - Caching animation: `https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/context-caching-animation.gif`
  - Report walkthrough GIF: `https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/report-walkthrough.gif`
  - results.json screenshot: `https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/results-json-screenshot.png`
- Links: https://github.com/PragmaTech-GmbH/spring-test-profiler and https://pragmatech.digital/products/spring-test-profiler/
- Current release: 0.2.3 (Java 17+, Spring Boot 2, 3, and 4)

---

## Variation 1: Problem-first ("Your Spring tests are slower than they need to be")

### Your Spring tests are slower than they need to be

Most Spring Boot test suites waste minutes per build on something entirely avoidable: starting ApplicationContexts that Spring could have reused.

Spring's TestContext framework caches contexts across tests. It fingerprints each test's configuration into a MergedContextConfiguration and reuses a cached context when the fingerprint matches. Same fingerprint, instant reuse. One tiny difference, and you pay for a full context startup. This animation from the report's theory section shows the mechanism:

![How Spring test context caching works](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/context-caching-animation.gif)

The problem: none of this is visible in your build output. That is what the Spring Test Profiler is for. It is a small test-scoped dependency that hooks into Spring Test with zero configuration and writes an HTML report (plus a JSON summary) after every test run, showing cache hits, misses, and where your context time goes.

Two new features make the invisible even harder to ignore:

**1. Context Cache Timeline (incubating)**

The report now draws a timeline of your context cache: one row per ApplicationContext, the saturated segment is the context load with its duration, the light segment is the time it stayed cached, dark bars are the tests running on it, and a red edge marks the moment a context was removed by @DirtiesContext or LRU eviction. Watch the walkthrough:

![Spring Test Profiler report walkthrough](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/report-walkthrough.gif)

The timeline is an incubating feature, so feedback is very welcome on [GitHub](https://github.com/PragmaTech-GmbH/spring-test-profiler/issues).

**2. results.json: stop context regressions in CI**

Fixing your context count once is good. Keeping it fixed is better. Next to the HTML report, the profiler now always writes a flat, jq-friendly `results.json` with the metrics only the profiler knows: contexts created, cache hits and misses, total context creation time, and potential savings.

![results.json example](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/results-json-screenshot.png)

Pin your expected context count and fail the build when someone accidentally introduces a new context, for example through a new @DirtiesContext or a subtle configuration difference:

```bash
expectedContexts=3
actualContexts=$(jq -r '.contextsCreated' target/spring-test-profiler/results.json)

if [ "$actualContexts" != "$expectedContexts" ]; then
  echo "Expected $expectedContexts Spring contexts but $actualContexts were created"
  exit 1
fi
```

Your test suite's context count becomes a guarded invariant instead of a slowly degrading number.

Get started with one dependency: [Spring Test Profiler on GitHub](https://github.com/PragmaTech-GmbH/spring-test-profiler) - more background on the [PragmaTech product page](https://pragmatech.digital/products/spring-test-profiler/).

---

## Variation 2: Release-notes style ("What's new in Spring Test Profiler")

### What's new in Spring Test Profiler

Quick refresher: the Spring Test Profiler is a test-scoped dependency that profiles your Spring Test execution with zero configuration. It focuses on Spring's TestContext context caching, the hidden mechanism that can cut your build time in half when it works and silently burn minutes when it does not. After each test run it writes a self-contained HTML report to `target/spring-test-profiler/latest.html` (Maven) or `build/spring-test-profiler/latest.html` (Gradle).

If context caching is new to you, this animation from the report explains it in 30 seconds:

![How Spring test context caching works](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/context-caching-animation.gif)

The two latest releases shipped two bigger features:

**Context Cache Timeline (incubating)**

A Gantt-style chart of your TestContext cache. Each row is one ApplicationContext: you see when it was loaded and how long that took, how long it stayed alive in the cache, which tests executed on it (dark bars), and when it was removed, with @DirtiesContext removals and LRU evictions marked by a red edge. The walkthrough below scrolls through a full report and ends on the timeline:

![Spring Test Profiler report walkthrough](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/report-walkthrough.gif)

The feature is incubating: the visualization works today and its details will evolve with your input. Feedback goes to the [issue tracker](https://github.com/PragmaTech-GmbH/spring-test-profiler/issues).

**results.json for CI assertions**

Every run now also writes `results.json` next to the HTML report: one flat JSON object with the profiler-specific metrics (contextsCreated, contextCacheHits, contextCacheMisses, totalContextCreationTimeMs, potentialTimeSavingsMs, and friends).

![results.json example](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/results-json-screenshot.png)

The main use case is protecting your optimization work: assert the context count in CI so nobody accidentally introduces a new context without noticing.

```bash
expectedContexts=3
actualContexts=$(jq -r '.contextsCreated' target/spring-test-profiler/results.json)

if [ "$actualContexts" != "$expectedContexts" ]; then
  echo "Expected $expectedContexts Spring contexts but $actualContexts were created"
  exit 1
fi
```

**Upgrading**: bump the dependency to the latest version, no other changes needed:

```xml
<dependency>
  <groupId>digital.pragmatech.testing</groupId>
  <artifactId>spring-test-profiler</artifactId>
  <version>0.2.3</version>
  <scope>test</scope>
</dependency>
```

All details on [GitHub](https://github.com/PragmaTech-GmbH/spring-test-profiler) and the [product page](https://pragmatech.digital/products/spring-test-profiler/).

---

## Variation 3: Tutorial / hands-on ("Guard your Spring context count in CI in 5 minutes")

### Guard your Spring context count in CI in 5 minutes

Here is a small CI recipe with a big payoff: fail the build when your Spring test suite starts creating more ApplicationContexts than it should.

Why care? Spring's TestContext framework caches ApplicationContexts across tests and reuses them when test configurations match. Every avoidable context is often 5 to 30 seconds of pure startup waste, and context count regressions creep in silently: a new @DirtiesContext here, a slightly different @MockitoBean setup there. This animation shows the caching mechanism:

![How Spring test context caching works](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/context-caching-animation.gif)

**Step 1**: add the [Spring Test Profiler](https://github.com/PragmaTech-GmbH/spring-test-profiler) as a test-scoped dependency. It activates itself through Spring's service loader, no configuration needed.

**Step 2**: run your tests. The profiler writes an HTML report plus a flat `results.json` to `target/spring-test-profiler/` (or `build/` for Gradle):

![results.json example](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/results-json-screenshot.png)

**Step 3**: pin the context count in your pipeline:

```bash
expectedContexts=3
actualContexts=$(jq -r '.contextsCreated' target/spring-test-profiler/results.json)

if [ "$actualContexts" != "$expectedContexts" ]; then
  echo "Expected $expectedContexts Spring contexts but $actualContexts were created"
  exit 1
fi
```

From now on, anyone who accidentally introduces a new context breaks the build instead of slowly breaking your build times. The same pattern works for time budgets, for example on `totalContextCreationTimeMs`.

**Bonus: see your context cache on a timeline.** To find out which contexts to eliminate in the first place, the report's new Context Cache Timeline (incubating) shows one row per ApplicationContext: load time, cache lifetime, the tests that ran on it, and red markers where contexts got removed by @DirtiesContext or LRU eviction.

![Spring Test Profiler report walkthrough](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/report-walkthrough.gif)

Try it on your suite and share feedback on the timeline via [GitHub issues](https://github.com/PragmaTech-GmbH/spring-test-profiler/issues). More on the profiler: [product page](https://pragmatech.digital/products/spring-test-profiler/).
