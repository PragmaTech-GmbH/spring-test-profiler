# LinkedIn Post: Context Cache Timeline Feature

- Purpose: announce the new (incubating) Context Cache Timeline in the Spring Test Profiler report
- Attach: `assets/context-timeline-screenshot.png`
- Link target: https://github.com/PragmaTech-GmbH/spring-test-profiler
- Feedback link: https://github.com/PragmaTech-GmbH/spring-test-profiler/issues
- Suggested hashtags: #Java #Spring #SpringBoot #Testing #DeveloperProductivity
- Note: LinkedIn truncates after roughly 200 characters before the "see more" fold, so the first two lines carry the hook

---

## Variation 1: Problem-first

Your Spring Boot test suite is probably rebuilding the same ApplicationContext over and over. You just cannot see it.

Spring's TestContext framework caches ApplicationContexts across tests. One tiny configuration difference, one @DirtiesContext too many, and Spring silently starts a brand-new context instead of reusing a cached one. That is where the minutes in your build go.

The Spring Test Profiler now visualizes this on a Context Cache Timeline (see the screenshot):

- One row per ApplicationContext in the cache
- The saturated segment shows the context load and how long it took
- The light segment shows how long the context stayed alive in the cache
- The dark bars are the actual test executions running on that context
- A red edge marks the moment a context was removed, whether through @DirtiesContext or LRU eviction

Suddenly the expensive context churn that was invisible in your build log is one glance away.

The timeline is an incubating feature and we are actively shaping it. If you try it on your test suite, I would love to hear what works and what is missing: https://github.com/PragmaTech-GmbH/spring-test-profiler/issues

Zero-config setup, one test dependency: https://github.com/PragmaTech-GmbH/spring-test-profiler

#Java #Spring #SpringBoot #Testing #DeveloperProductivity

---

## Variation 2: Feature-first show-and-tell

This is what your Spring TestContext cache is actually doing during a build.

The new Context Cache Timeline in the Spring Test Profiler turns the invisible lifecycle of your ApplicationContexts into a Gantt-style chart:

- Each row is one ApplicationContext in Spring's TestContext cache
- Saturated segment: the context load, labeled with its duration (the expensive part)
- Light segment: the time the context stayed cached and reusable
- Dark bars: the individual test classes executing on that context
- Red edge: the context got removed from the cache, either by @DirtiesContext or by LRU eviction once the cache exceeds its maximum size

In the attached screenshot you can immediately spot the test that dirtied its context after 1.9 seconds of startup work, and the contexts that were loaded once and reused nicely.

Setup is one test-scoped dependency, no configuration. Run your tests and open target/spring-test-profiler/latest.html.

The timeline is incubating: it works, it is useful, and it will evolve based on your feedback. Tell us what you think: https://github.com/PragmaTech-GmbH/spring-test-profiler/issues

Repo: https://github.com/PragmaTech-GmbH/spring-test-profiler

#Java #Spring #SpringBoot #Testing #DeveloperProductivity

---

## Variation 3: Story / behind-the-scenes

We had the numbers. We were still blind.

The Spring Test Profiler has always reported context cache hits, misses, and hit ratios for Spring Boot test suites. Useful, but a number like "11 contexts created, 42% hit rate" does not answer the questions that actually matter:

When was each context created? How long did it live? Which tests ran on it? And why did it disappear from the cache?

So we built a timeline. One row per ApplicationContext, showing its load time, its lifetime in the cache, every test execution on it as a dark bar, and a red edge when it got evicted or killed by @DirtiesContext. The screenshot shows a real run: you can literally see one test burning 1.9 seconds of context startup and then throwing the context away.

We are shipping it as an incubating feature on purpose. The core visualization works, but the details (parallel execution, bigger suites, more removal causes) will evolve with real-world feedback. That is the ask: run it on your test suite and tell us what you see, what confuses you, and what is missing.

Feedback: https://github.com/PragmaTech-GmbH/spring-test-profiler/issues
Get started (one dependency, zero config): https://github.com/PragmaTech-GmbH/spring-test-profiler

#Java #Spring #SpringBoot #Testing #OpenSource
