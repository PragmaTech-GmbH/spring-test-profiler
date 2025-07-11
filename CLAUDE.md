# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Test Insight is a JUnit Jupiter extension that provides visualization and insights for Spring Test execution, with a focus on Spring context caching statistics.

### Architecture

- **Core Extension** (`SpringTestInsightExtension`): JUnit Jupiter extension implementing TestWatcher and lifecycle callbacks
- **Statistics Collection**: Context cache tracking integrated into SpringTestInsightListener
- **Report Generation** (`TestExecutionReporter`): Generates HTML reports with test results and caching metrics
- **Data Models**: POJOs for test execution data and statistics

### Key Design Decisions

1. **Service Loader Integration**: Automatic activation via META-INF/services configuration
2. **Thread-Safe Collections**: Uses ConcurrentHashMap for concurrent test execution
3. **HTML Report Generation**: Self-contained HTML with embedded CSS/JS for portability
4. **Extension Composition**: Works alongside Spring's extensions without interference

## Common Development Tasks

### Building the Project

```bash
cd spring-test-profiler-extension
./mvnw clean install
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SpringContextStatisticsTest

# Run integration tests only
./mvnw verify
```

### Running the Demo

```bash
# First install the extension
cd spring-test-profiler-extension
./mvnw clean install

# Then run demo tests
cd ../demo
mvn clean test

# View report at: target/spring-test-profiler/latest.html
```

## Code Structure

```
spring-test-profiler-extension/
├── src/main/java/digital/pragmatech/springtestinsight/
│   ├── SpringTestInsightExtension.java    # Main extension entry point
│   ├── SpringTestInsightListener.java     # Test execution listener with cache tracking
│   ├── TestExecutionReporter.java         # HTML report generator
│   └── [Data models]                      # Test data structures
└── src/test/java/                         # Comprehensive test suite

demo/                                       # Spring Boot demo application
├── src/main/java/                         # Demo app with REST API
└── src/test/java/                         # Tests showcasing the extension
```

## Testing Approach

- Unit tests for individual components (statistics, data models)
- Integration tests with real Spring contexts
- Demo project provides realistic usage examples
- Tests demonstrate various Spring test annotations (@DataJpaTest, @WebMvcTest, @SpringBootTest)
