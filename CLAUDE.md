# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Test Profiler is a JUnit Jupiter extension that provides visualization and insights for Spring Test execution, with a focus on Spring context caching statistics. The goal is to identify optimization opportunities in Spring Test suites to speed up builds.

### Architecture

- **Core Extension** (`SpringTestProfilerListener`): JUnit Jupiter TestExecutionListener with HIGHEST_PRECEDENCE
- **Context Profiling** (`TimingTrackingApplicationContextInitializer`): Deep integration into Spring context loading with timing and bean creation metrics
- **Statistics Collection**: Multi-layered tracking including Spring's internal cache and custom metrics beyond the 32-context limit
- **Report Generation** (`TestExecutionReporter`): Self-contained HTML reports with embedded CSS/JS and optional JSON output
- **Service Loader Integration**: Automatic activation via META-INF/spring.factories

### Key Design Decisions

1. **Auto-Discovery**: Uses Spring's service loader pattern for zero-configuration activation
2. **Thread-Safe Collections**: ConcurrentHashMap and AtomicInteger for parallel test execution support
3. **Non-Intrusive Integration**: Works via reflection to access Spring internals without breaking encapsulation
4. **Comprehensive Tracking**: Independent tracking supplements Spring's built-in cache statistics
5. **Build Tool Agnostic**: Automatic detection and appropriate report placement for Maven/Gradle

## Common Development Tasks

### Building the Project

```bash
./mvnw clean install
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SpringTestProfilerListenerTest

# Run integration tests
./mvnw verify

# Run tests with profiler enabled
./mvnw test -Dspring.test.insight.enabled=true
```

### Running Demo Applications

```bash
# Install the extension first
./mvnw clean install

# Run different demo scenarios
cd demo/spring-boot-3.5-maven
mvn clean test

cd ../spring-boot-3.5-maven-junit-parallel
mvn clean test

cd ../spring-boot-3.5-maven-failsafe-parallel
mvn clean verify

# View reports at: target/spring-test-profiler/latest.html
```

### Development Testing

```bash
# Test with JSON output enabled
./mvnw test -Dspring.test.insight.json.beta=true

# Test with custom report directory
./mvnw test -Dspring.test.insight.report.dir=/custom/path
```

## Code Structure

```
src/main/java/digital/pragmatech/testing/
├── SpringTestProfilerListener.java          # Main entry point, test lifecycle hooks
├── TimingTrackingApplicationContextInitializer.java  # Context profiling integration
├── tracking/
│   ├── TestExecutionTracker.java           # Test execution metrics
│   ├── ContextCacheTracker.java            # Custom context cache tracking
│   └── SpringContextCacheAccessor.java     # Reflection-based Spring cache access
├── profiling/
│   ├── ContextProfileData.java             # Context loading profile data
│   └── BeanCreationProfiler.java           # Bean creation timing metrics
├── reporting/
│   ├── html/TestExecutionReporter.java     # HTML report generation
│   └── json/JsonReportGenerator.java       # JSON report output (beta)
└── util/
    ├── BuildToolDetection.java             # Maven/Gradle detection
    └── VersionInfo.java                     # Extension version information

src/main/resources/
├── META-INF/spring.factories                # Service loader configuration
└── templates/                              # Thymeleaf report templates

demo/                                        # Multiple demo projects
├── spring-boot-3.4-maven/                  # Basic Maven setup
├── spring-boot-3.5-maven/                  # Latest Spring Boot
├── spring-boot-3.5-maven-junit-parallel/   # JUnit parallel execution
├── spring-boot-3.5-maven-failsafe-parallel/ # Failsafe parallel execution
└── spring-boot-3.5-gradle/                 # Gradle build
```

## Integration Architecture

### Automatic Activation Flow
1. Service Loader discovers extension via META-INF/spring.factories
2. Spring registers TestExecutionListener and ApplicationContextInitializer
3. Listener hooks into test lifecycle with highest precedence
4. Context initializer profiles Spring context creation with detailed metrics
5. Shutdown hook generates reports when JVM exits

### Data Collection Layers
- **Test Execution**: Class/method timing, status tracking, test lifecycle events
- **Context Cache**: Spring's internal cache statistics plus custom tracking beyond limits
- **Context Profiling**: Deep timing, memory usage, and bean creation metrics during context loading
- **Optimization Analysis**: Similarity scoring and harmonization recommendations

### Report Generation
- **HTML Reports**: Self-contained with embedded CSS, timeline visualizations, optimization recommendations
- **JSON Reports**: Structured data output (beta feature, enabled via system property)
- **Build Integration**: Automatic placement in target/build directories with latest.html symlink

## Testing Approach

- Unit tests for individual components (statistics, tracking, data models)
- Integration tests with real Spring contexts and various test annotations
- Demo projects provide realistic usage examples across different Spring Boot versions
- Tests cover sequential and parallel execution modes (JUnit parallel, Maven Failsafe parallel)
- Comprehensive coverage of different Spring test slices (@DataJpaTest, @WebMvcTest, @SpringBootTest)