# Demo Projects

The [`demo/`](https://github.com/PragmaTech-GmbH/spring-test-profiler/tree/main/demo) directory contains standalone example projects that show the profiler in realistic setups. Each demo is a complete Spring Boot application with tests - use them as a reference for integrating the profiler into your own build.

## Available Demos

| Demo | Spring Boot | Build Tool | Demonstrates | Verified in CI |
|---|---|---|---|---|
| `spring-boot-2.7-maven` | 2.7 (Spring Framework 5) | Maven | Backward compatibility with Spring Boot 2 | ✅ |
| `spring-boot-3.4-maven` | 3.4 | Maven | Standard Spring Boot 3.4 setup | ✅ |
| `spring-boot-3.5-maven` | 3.5 | Maven | Baseline Spring Boot 3.5 setup | ✅ |
| `spring-boot-3.5-gradle` | 3.5 | Gradle | Gradle integration, report under `build/` | ✅ |
| `spring-boot-3.5-maven-multimodule` | 3.5 | Maven | Profiler in a multi-module build | - |
| `spring-boot-3.5-maven-junit-parallel` | 3.5 | Maven | JUnit parallel execution (not yet supported, see [limitations](Troubleshooting-and-Limitations.md)) | - |
| `spring-boot-3.5-maven-failsafe-parallel` | 3.5 | Maven | Failsafe parallel execution (not yet supported, see [limitations](Troubleshooting-and-Limitations.md)) | - |
| `spring-boot-4.0-maven` | 4.0 (Spring Framework 7) | Maven | Forward compatibility with Spring Boot 4 | ✅ |

## Running a Demo

The demos consume the profiler from your local Maven repository, so install it first from the repository root:

```bash
./mvnw clean install
```

Then run any demo:

```bash
# Maven demo
cd demo/spring-boot-3.5-maven
mvn clean verify
# Report: target/spring-test-profiler/latest.html

# Gradle demo
cd demo/spring-boot-3.5-gradle
./gradlew build
# Report: build/spring-test-profiler/latest.html
```
