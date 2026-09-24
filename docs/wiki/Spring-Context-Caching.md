# Spring Context Caching

This page explains the Spring mechanism the profiler is built around. Understanding it makes the report much easier to act on.

## How the TestContext Cache Works

Starting a Spring application context is expensive - often several seconds per context for a real application. To avoid paying that cost for every test class, the Spring TestContext Framework caches application contexts **within one JVM test run** and reuses them across test classes.

The animation below (also shown in the report's theory section) illustrates the mechanism:

![Animation explaining Spring test context caching: Spring scans the test configuration, builds a cache key from the MergedContextConfiguration hashCode, and reuses matching ApplicationContexts](https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/context-caching-animation.gif)

Step by step, this is what happens for every Spring test class:

1. **Configuration scan** - before running a test class, Spring X-rays its complete test configuration: annotations like `@SpringBootTest`, `@ContextConfiguration`, `@ActiveProfiles`, `@TestPropertySource`, plus everything contributed indirectly, such as context customizers from `@MockBean` or Testcontainers/WireMock integrations.
2. **Merge into one object** - all these customization points are merged into a single `MergedContextConfiguration` instance that fully describes the context this test class needs.
3. **Cache key from hashCode** - the `hashCode` of that `MergedContextConfiguration` (backed by `equals`) acts as the cache key into the context cache.
4. **Hit or miss** - if a context with the same key already exists in the cache, the test reuses it instantly (cache hit). If not, Spring starts a brand-new application context - often the slowest step of the whole test class - and stores it under the new key (cache miss).

The crucial consequence: same key means instant reuse, while **one tiny difference** in any configuration attribute means a slow, brand-new context. There is no "close enough" - the key either matches exactly or it does not.

Whether a context can be reused is therefore decided entirely by the attributes that flow into the `MergedContextConfiguration`. Roughly, two test classes share a context only when **all** of the following match:

- Context configuration classes and locations (`@ContextConfiguration`, `@SpringBootTest` classes)
- Active profiles (`@ActiveProfiles`)
- Property sources and inlined properties (`@TestPropertySource`, `properties` attributes)
- Context initializers
- Context customizers (added by annotations like `@MockBean`, `@DynamicPropertySource`, WireMock integrations, Testcontainers integrations, and so on)
- Parent context, context loader, and web configuration

If a single attribute differs, Spring creates and caches a **new** context.

## Why Your Cache Hit Rate Is Low

Common, often accidental, causes for contexts not being reused:

- `@DirtiesContext` - marks the context as dirty and forces a fresh one afterwards
- `@MockBean` / `@MockitoBean` on individual test classes - each distinct set of mocked beans creates a distinct context customizer, and therefore a distinct context
- Different `@ActiveProfiles` or `@TestPropertySource` values across otherwise identical test classes
- Different test slices (`@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`) - by design these produce different contexts, which is fine, but mixing arbitrary configurations within a slice multiplies contexts
- Dynamic values in the configuration, such as random ports or per-test property values

The report's [context configuration diff view](Understanding-the-Report.md#spring-context-configurations) shows exactly which attribute differs between two contexts.

## The Cache Size Limit

Spring's context cache holds **32 contexts by default** (configurable via the `spring.test.context.cache.maxSize` property). When the limit is exceeded, the least recently used context is closed and evicted. Test suites with many distinct configurations can silently thrash this cache: contexts get evicted and re-created, and Spring's built-in statistics no longer tell the full story.

The Spring Test Profiler tracks context usage **independently** of Spring's internal cache, so its metrics stay complete even when your suite creates more contexts than the cache can hold.

## What Good Looks Like

- A handful of distinct contexts, deliberately chosen (for example: one full `@SpringBootTest` context, one `@WebMvcTest` slice, one `@DataJpaTest` slice)
- A high cache hit rate - most test classes reuse an existing context
- Context creation clustered at the start of the run rather than spread throughout

To move your suite in this direction, follow the recommendations in the report's [optimization section](Understanding-the-Report.md#optimization-recommendations).

## Further Reading

- [Spring Framework reference: Context Caching](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html)
- [Understanding the Report](Understanding-the-Report.md)
