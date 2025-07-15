# Spring Test Profiler

![](docs/resources/spring-test-profiler-logo-three-256x256.png)

A JUnit Jupiter extension that provides visualization and insights for Spring Test execution, with a focus on Spring context caching statistics.


[![Build & Test Maven Project (main)](https://github.com/PragmaTech-GmbH/spring-test-profiler/workflows/CI/badge.svg)](https://github.com/PragmaTech-GmbH/spring-test-profiler/actions/workflows/ci.yml?query=branch%3Amain)

## Features

- Track Spring Test context caching statistics for your test suite
- Show context reuse metrics and cache hit/miss ratios
- Identify tests that couldn't reuse contexts and explain why
- Easy integration with a `spring.factories` file or `@TestExecutionListeners` annotation
- Works with both Maven Surefire/Failsafe and Gradle test tasks

## Requirements

This profiler works with Java 17+ and is compatible with Spring Framework 6.X (aka. Spring Boot 3.X).

## Prototype Phase

This project is highly work-in-progress. What's currently not working or missing:

- Support for parallel test execution
- Fully-fledged visualization of the contexts on a timeline
- For each Gradle test task, a separate HTML report is generated
- For Surefire and Failsafe, a separate HTML report is generated

## Usage

[![](https://img.shields.io/badge/Latest%20Version-0.9.0-orange)](/spring-test-profiler-extension/pom.xml)

### 1. Add the Dependency

#### Quick Start Maven

Add the dependency to your project:

```xml
<dependency>
  <groupId>digital.pragmatech.testing</groupId>
  <artifactId>spring-test-profiler</artifactId>
  <version>0.9.0</version>
  <scope>test</scope>
</dependency>
```

#### Quick Start Gradle

Add the dependency to your project:

```groovy
testImplementation('digital.pragmatech.testing:spring-test-profiler:0.9.0')
```


### 2. Activate the Profiler

Pick **either one** of the following methods to activate the profiler in your tests.

#### Automatically for all Your Tests (Recommended)

Add a file named `META-INF/spring.factories` to your resources directory with the following content:

```text
org.springframework.test.context.TestExecutionListener=\
digital.pragmatech.testing.SpringTestInsightListener
```

#### Manually for Specific Tests

Add the `@TestExecutionListeners` annotation to your test classes:

```java
@TestExecutionListeners(
  value = {SpringTestInsightListener.class},
  mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
```

### 3. Run Your Tests

Execute your tests:

```bash
# Maven
./mvnw verify

# Gradle
./gradlew build
```

### 4. Analyze the Generated Report

After test execution, find the HTML report at:

- Maven: `target/spring-test-profiler/latest.html`
- Gradle: `build/spring-test-profiler/latest.html`

### Demo Report



## Bug Reports

Found a bug? Please help us improve by reporting it:

1. **Search existing issues** at https://github.com/rieckpil/spring-test-profiler/issues
2. **Create a new issue** with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Java/Spring/JUnit versions
   - Relevant log output or screenshots

## Contributing

We welcome contributions! Here's how to get started:

### Development Setup

1. **Fork and clone** the repository
2. **Build the project**:

```bash
cd spring-test-profiler-extension
./mvnw install
```

3. **Run tests**:

```bash
./mvnw test
```
