# Spring Test Profiler - Voiceover Script

Total duration: ~2:40

---

## Slide 1: Title (0:00 - 0:15)

> Did you know Spring has a built-in context caching mechanism for tests? Most developers don't — and their builds pay the price. Let me show you how to unlock this hidden gem with the Spring Test Profiler.

---

## Slide 2: The Problem (0:15 - 0:43)

> Here's the problem. Every time your tests use a unique Spring configuration, Spring creates a brand new application context — and that takes anywhere from two to ten seconds. Tiny differences between test classes, like an extra profile or a different property, silently create duplicate contexts that could have been shared. Add @DirtiesContext into the mix, and you're destroying and recreating contexts for every single test method. The worst part? There's no built-in way to see what's happening inside Spring's context cache. It's a complete black box.

---

## Slide 3: Setup (0:43 - 1:06)

> Setting up the profiler takes three steps. First, add the test dependency to your pom.xml — or build.gradle if you prefer. Second, register the listener and initializer in a spring.factories file under your test resources. And third — just run your tests as you normally would. The profiler hooks in automatically and generates an HTML report at target/spring-test-profiler/latest.html.

---

## Slide 4: Transition (1:06 - 1:11)

> Let's see what that report looks like.

---

## Report Walkthrough (1:11 - 2:17)

### Summary Section (1:11 - 1:25)

> The report opens with a summary dashboard. Here you can see how many contexts were created, how many cache hits and misses occurred, and your overall cache hit rate. This immediately tells you how efficiently your test suite is reusing contexts.

### Cache Entries (1:25 - 2:00)

> Scrolling down, we get to the cache entries. Each entry represents a unique Spring context. You can sort by load time or number of tests. Notice this first context — it's shared across multiple test classes. That's good, that's what we want. But look at this one — it was created by a test using @DirtiesContext, which forces a fresh context every time. You can see the load time right here, so you know exactly how much time each context costs your build.

### Context Comparison (2:00 - 2:17)

> The context comparison visualizer is where it gets really powerful. You can select two contexts and see exactly what's different between them. Often it's just a single property or an extra profile — a small change that's easy to fix but was costing you seconds on every test run.

---

## Slide 5: Closing (2:17 - 2:39)

> Getting started takes thirty seconds. Add the dependency, register in spring.factories, run your tests, and open the report. You'll immediately see where your build time is being wasted and what to fix. The project is open source under the MIT license, and it works with Spring Boot 2 through 4. If this is useful to you, give us a star on GitHub — link is in the description. Thanks for watching!
