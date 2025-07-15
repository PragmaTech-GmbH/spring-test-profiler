# Spring Test Profiler

![](docs/resources/spring-test-profiler-logo-three-256x256.png)

A JUnit Jupiter extension that provides visualization and insights for Spring Test execution, with a focus on Spring context caching statistics.


[![Build & Test Maven Project (main)](https://github.com/PragmaTech-GmbH/spring-test-profiler/workflows/CI/badge.svg)](https://github.com/PragmaTech-GmbH/spring-test-profiler/actions/workflows/ci.yml?query=branch%3Amain)

## Features

- Visualize test results in a clean HTML report
- Track Spring Test context caching statistics
- Show context reuse metrics and cache hit/miss ratios
- Identify tests that couldn't reuse contexts and explain why
- Support for Spring 6+ and Java 21+
- Easy integration via `@ExtendWith` or automatic service loader activation
- Works with both Maven Surefire/Failsafe and Gradle test tasks

## Usage

[![](https://img.shields.io/badge/Latest%20Version-0.9.0-orange)](/spring-test-profiler-extension/pom.xml)

### Quick Start Maven

Add the dependency to your project:

```xml
<dependency>
  <groupId>digital.pragmatech.testing</groupId>
  <artifactId>spring-test-profiler</artifactId>
  <version>0.9.0</version>
  <scope>test</scope>
</dependency>
```


### Quick Start Gradle

Add the dependency to your project:

```groovy
testImplementation('digital.pragmatech.testing:spring-test-profiler:0.9.0')
```


### Automatic Activation (Recommended)

TBD

### Running Tests

Execute your tests normally:

```bash
# Maven
mvn test

# Gradle
./gradlew test
```

### Viewing Reports

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
