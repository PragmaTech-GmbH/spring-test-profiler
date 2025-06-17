# Spring Test Insight Demo

This demo project showcases the Spring Test Insight Extension in action.

## Overview

This is a simple Spring Boot application with:

- REST API for user management
- JPA/Hibernate with H2 in-memory database
- Comprehensive test suite demonstrating Spring Test context caching

## Running the Tests

First, install the Spring Test Insight Extension to your local Maven repository:

```bash
cd ../spring-test-insight-extension
./mvnw clean install
```

Then run the demo tests:

```bash
cd ../demo
mvn clean test
```

## Viewing the Test Report

After running the tests, the Spring Test Insight report will be generated at:

- `target/spring-test-insight/latest.html`

Open this file in a web browser to see:

- Test execution summary
- Spring context caching statistics
- Cache hit/miss rates
- Individual test results with execution times
- Failed test details with stack traces

## Test Structure

The demo includes various types of tests:

- `UserRepositoryTest` - @DataJpaTest for repository layer
- `UserServiceTest` - Unit tests with mocks
- `UserControllerTest` - @WebMvcTest for REST controllers
- `UserIntegrationTest` - Full @SpringBootTest integration tests

Each test class uses different Spring configurations, demonstrating how the extension tracks context caching across
different test types.

## Key Features Demonstrated

1. **Context Caching Visualization**: See which test classes share Spring contexts
2. **Performance Metrics**: Track context load times and test execution durations
3. **Cache Efficiency**: Monitor cache hit rates to optimize test suite performance
4. **Test Results**: Comprehensive view of passed, failed, and skipped tests

## Tips for Optimization

Based on the report, you can:

- Identify tests that create new contexts unnecessarily
- Group tests with similar configurations to improve cache reuse
- Find slow context initialization that impacts test performance
