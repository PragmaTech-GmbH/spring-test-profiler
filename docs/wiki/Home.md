# Spring Test Profiler

Welcome to the Spring Test Profiler documentation. This wiki is the single source of truth for everything beyond the [README quick start](https://github.com/PragmaTech-GmbH/spring-test-profiler#usage).

Spring's `TestContext` context caching is one of the most unknown hidden gems of testing with Spring Boot - it can cut your build times in half, and often even more. The Spring Test Profiler visualizes how your test suite uses (or misses) this cache and points you to concrete optimization opportunities.

<p align="center">
  <img src="https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/report-top.png" alt="Spring Test Profiler Report" />
</p>

## Where to Go

| Page | What You Will Find |
|---|---|
| [Getting Started](Getting-Started.md) | Installation, activation, and your first report |
| [Understanding the Report](Understanding-the-Report.md) | A walkthrough of every report section |
| [Spring Context Caching](Spring-Context-Caching.md) | Background on how the `TestContext` cache works and why contexts differ |
| [Configuration Reference](Configuration-Reference.md) | All system properties and report output locations |
| [Advanced Usage](Advanced-Usage.md) | Context customizer extensions, custom report directories, JSON reports |
| [New Features](New-Features.md) | Curated highlights of recent releases |
| [Demo Projects](Demo-Projects.md) | Ready-to-run example projects for every supported setup |
| [Troubleshooting and Limitations](Troubleshooting-and-Limitations.md) | Known limitations, common issues, and how to report bugs |
| [Contributing](Contributing.md) | Development setup and contribution guidelines |

## Requirements at a Glance

The profiler works with Java 17+ and is compatible with:

- Spring Framework 5 (Spring Boot 2)
- Spring Framework 6 (Spring Boot 3)
- Spring Framework 7 (Spring Boot 4)

It supports both Maven (Surefire/Failsafe) and Gradle test tasks.

## Links

- [Product page and demo report](https://pragmatech.digital/products/spring-test-profiler/)
- [GitHub repository](https://github.com/PragmaTech-GmbH/spring-test-profiler)
- [GitHub releases](https://github.com/PragmaTech-GmbH/spring-test-profiler/releases)
- [Issue tracker](https://github.com/PragmaTech-GmbH/spring-test-profiler/issues)
