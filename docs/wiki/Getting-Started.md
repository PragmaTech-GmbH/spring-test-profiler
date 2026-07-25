# Getting Started

This guide takes you from zero to your first Spring Test Profiler report.

## Prerequisites

- Java 17+
- Spring Framework 5, 6, or 7 (Spring Boot 2, 3, or 4)
- Maven or Gradle

## 1. Add the Dependency

The profiler is only needed at test runtime, so add it with test scope.

### Maven

```xml
<dependency>
  <groupId>digital.pragmatech.testing</groupId>
  <artifactId>spring-test-profiler</artifactId>
  <version>0.2.1</version>
  <scope>test</scope>
</dependency>
```

### Gradle

```groovy
testRuntimeOnly("digital.pragmatech.testing:spring-test-profiler:0.2.1")
```

## 2. Activate the Profiler

Pick **either one** of the following methods.

### Automatically for All Your Tests (Recommended)

Add a file named `META-INF/spring.factories` to your test resources directory (for example `src/test/resources/META-INF/spring.factories`) with the following content:

```text
org.springframework.test.context.TestExecutionListener=\
digital.pragmatech.testing.SpringTestProfilerListener
org.springframework.context.ApplicationContextInitializer=\
digital.pragmatech.testing.diagnostic.ContextDiagnosticApplicationInitializer
```

Spring's service loader mechanism picks this up automatically - no code changes required.

### Manually for Specific Tests

Add the `@TestExecutionListeners` and `@ContextConfiguration` annotations to your test classes:

```java
@TestExecutionListeners(
  value = {SpringTestProfilerListener.class},
  mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@ContextConfiguration(initializers = ContextDiagnosticApplicationInitializer.class)
```

This needs to be done for each test class where you want to use the profiler. Preferably, place these annotations on a central abstract integration test class, or use the automatic activation method above.

## 3. Run Your Tests

```bash
# Maven
./mvnw verify

# Gradle
./gradlew build
```

## 4. Open the Report

After test execution, find the HTML report at:

- Maven: `target/spring-test-profiler/latest.html`
- Gradle: `build/spring-test-profiler/latest.html`

The report is fully self-contained (CSS and JavaScript are inlined), so you can open it directly in a browser, archive it as a CI artifact, or share it with your team.

Next to the HTML report, a flat JSON summary (`results.json`) is written for machine consumption - see [Advanced Usage](Advanced-Usage.md#json-summary-report) for the available metrics and how to guard your context count in CI.

## Next Steps

- Learn what each section of the report means: [Understanding the Report](Understanding-the-Report.md)
- Understand the underlying mechanics: [Spring Context Caching](Spring-Context-Caching.md)
- Tune output locations and formats: [Configuration Reference](Configuration-Reference.md)
- Explore working examples: [Demo Projects](Demo-Projects.md)
