# Spring Test Profiler - Voiceover Script

Total duration: ~3:00

---

## Slide 1: Title (0:00 - 0:15)

> Did you know Spring has a built-in context caching mechanism for tests? Most developers don't - and their builds pay the price. Let me show you how to unlock this hidden gem with the Spring Test Profiler.

---

## Slide 2: Build Time Growth (0:15 - 0:35)

> Look at what happens as your test suite grows. On the left, without context caching - every new integration test creates its own Spring context, and your build time grows linearly. Twenty test classes, twenty context startups, easily minutes of wasted time. On the right, with proper context caching - new tests reuse existing contexts, and build time barely increases. That's the difference between a five-minute build and a thirty-second one.

---

## Slide 3: The Problem (0:35 - 1:00)

> So why doesn't this just work out of the box? Because small configuration differences between test classes silently break caching. An extra profile here, a different property there - each one creates a separate context. And if you're using @DirtiesContext, you're destroying the cache after every test method. The worst part - there's no built-in way to see what's happening inside Spring's context cache. It's a complete black box.

---

## Slide 4: Setup (1:00 - 1:25)

> Setting up the profiler takes three steps. First, add the test dependency to your pom.xml - or build.gradle if you prefer. Second, register the listener and initializer in a spring.factories file under your test resources. And third - just run your tests as you normally would. The profiler hooks in automatically and generates an HTML report at target/spring-test-profiler/latest.html.

---

## Slide 5: Transition (1:25 - 1:30)

> Let's see what that report looks like.

---

## Report Walkthrough (1:30 - 2:35)

### Summary Section (1:30 - 1:45)

> The report opens with a summary dashboard. Here you can see how many contexts were created, how many cache hits and misses occurred, and your overall cache hit rate. This immediately tells you how efficiently your test suite is reusing contexts.

### Cache Entries (1:45 - 2:15)

> Scrolling down, we get to the cache entries. Each entry represents a unique Spring context. You can sort by load time or number of tests. Notice this first context - it's shared across multiple test classes. That's good, that's what we want. But look at this one - it was created by a test using @DirtiesContext, which forces a fresh context every time. You can see the load time right here, so you know exactly how much time each context costs your build.

### Context Comparison (2:15 - 2:35)

> The context comparison visualizer is where it gets really powerful. You can select two contexts and see exactly what's different between them. Often it's just a single property or an extra profile - a small change that's easy to fix but was costing you seconds on every test run.

---

## Slide 6: Closing (2:35 - 3:00)

> Getting started takes thirty seconds. Add the dependency, register in spring.factories, run your tests, and open the report. You'll immediately see where your build time is being wasted and what to fix. The project is open source under the MIT license, and it works with Spring Boot 2 through 4. If this is useful to you, give us a star on GitHub - link is in the description. Thanks for watching!
