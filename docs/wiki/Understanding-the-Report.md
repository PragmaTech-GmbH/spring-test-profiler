# Understanding the Report

After a test run, open `target/spring-test-profiler/latest.html` (Maven) or `build/spring-test-profiler/latest.html` (Gradle). This page walks through the report from top to bottom.

You can also explore a hosted [demo report](https://pragmatech.digital/products/spring-test-profiler/) without running anything.

<p align="center">
  <img src="https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/report-full.png" alt="Full Spring Test Profiler Report" width="600" />
</p>

## Test Execution Summary

The top of the report shows the overall picture of your test run: how many test classes and methods ran, their outcomes, the total execution time, and the total number of Spring contexts created. Use this as a baseline - the goal of every optimization is to bring the total time and the context count down without losing coverage.

## How Context Caching Works (Theory Section)

The report includes a theory section that explains Spring's context caching mechanism with an animation: Spring scans the test configuration, builds a cache key from the `MergedContextConfiguration` hashCode, and reuses matching application contexts. For the full background, see [Spring Context Caching](Spring-Context-Caching.md).

## Spring Context Caching Statistics

This section surfaces the core caching metrics:

- **Cache hits and misses** - how often a test could reuse an already-loaded application context versus having to start a new one
- **Context load times** - how expensive each context creation was
- **Cache hit rate** - the single most important number: a low hit rate usually means your test configurations differ in ways that prevent reuse

The profiler tracks these metrics independently of Spring's internal statistics, so the numbers stay accurate even beyond Spring's default cache size limit of 32 contexts (see [Spring Context Caching](Spring-Context-Caching.md)).

## Context Cache Entries

Each cached application context is listed with the test classes that used it, its configuration attributes, and its load time. Sorting controls let you order entries, for example by load time, to find the most expensive contexts first.

## Spring Context Configurations

When two contexts look similar but were not shared, this section shows an attribute-by-attribute comparison of their `MergedContextConfiguration` - profiles, property sources, context initializers, context customizers, and more. The diff view aligns attributes across contexts so you can spot the exact difference that caused Spring to create a separate context.

If a context customizer's default description is too generic to explain a difference (common with WireMock and similar tools), you can plug in a [context customizer extension](Advanced-Usage.md#custom-context-customizer-descriptions) to render meaningful details.

## Context Lifecycle Timeline

The timeline visualizes the context cache over time: when each context was created, when contexts were removed from the cache, and how test execution proceeded around them. This helps you see context creation bursts, for example at the start of an integration test phase, and cache evictions caused by the cache size limit.

Note: the timeline visualization is still evolving - see [Troubleshooting and Limitations](Troubleshooting-and-Limitations.md).

## Optimization Recommendations

The report closes with actionable guidance:

- **Top optimization opportunities** - test classes or configurations where harmonizing settings would let contexts be shared
- **Potential impact analysis** - an estimate of how much build time you could save
- **Optimization tips** - general practices for maximizing context reuse

Typical wins include removing unnecessary `@DirtiesContext`, consolidating `@MockBean`/`@MockitoBean` usage into shared test configurations, and aligning active profiles and property sources across test classes.
