# Troubleshooting and Limitations

## Prototype Phase

> [!WARNING]
> This project is highly work-in-progress and should be considered a prototype to gather feedback and ideas for future development.

## Known Limitations

- **Parallel test execution is not yet supported.** Running tests in parallel (JUnit parallel execution or Surefire/Failsafe forking) can produce incomplete or inaccurate reports.
- **The context timeline visualization is not fully fledged yet.** Expect gaps and rough edges in the timeline section of the report.
- **Gradle: one report per test task.** Each Gradle test task generates its own HTML report instead of one combined report.
- **Maven: separate reports for Surefire and Failsafe.** Unit tests (Surefire) and integration tests (Failsafe) each produce their own report.

## Common Issues

### No Report Is Generated

1. Verify the dependency is on the **test** classpath (`<scope>test</scope>` for Maven, `testRuntimeOnly` for Gradle).
2. Verify activation: either the `META-INF/spring.factories` file is in your test resources, or the annotations are present on your test classes - see [Getting Started](Getting-Started.md#2-activate-the-profiler).
3. Make sure at least one **Spring** test ran (a plain unit test without a Spring context does not trigger the profiler).
4. Check for a custom `pragmatech.spring.test.insight.report.dir` property pointing somewhere unexpected - see [Configuration Reference](Configuration-Reference.md).

### The Report Shows Fewer Contexts Than Expected

Spring's context cache holds 32 contexts by default and evicts least recently used entries beyond that. The profiler tracks contexts independently of this limit, but if numbers still look off, check whether your tests ran in parallel (see limitations above).

### Two Contexts Look Identical but Are Not Shared

Open the context configuration comparison in the report - one attribute (profiles, properties, customizers, initializers) will differ. If the differing attribute is a context customizer that renders only as a class name, add a [context customizer extension](Advanced-Usage.md#custom-context-customizer-descriptions) to expose its configuration details. Background: [Spring Context Caching](Spring-Context-Caching.md).

## Reporting Bugs

Found a bug? Please help us improve by reporting it:

1. **Search existing issues** at https://github.com/PragmaTech-GmbH/spring-test-profiler/issues
2. **Create a new issue** with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Java/Spring/JUnit versions
   - Relevant log output or screenshots

A minimal reproducer project is the fastest path to a fix - consider forking one of the [demo projects](Demo-Projects.md) as a starting point.
