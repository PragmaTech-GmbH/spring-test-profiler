# spring-test-insight

## Requirements

- JUnit Jupiter Extension for Java 21+
- Visualize the test results in a nice looking HTML after the run
- Support Surefire/Failsafe and Gradle task reports to differentiate both test runs
- Report statistic about the Spring Test ContextCaching: How many context were created, how many were reused, how many were cached
- Show which test could not reuse a context and explain why
- Make it support Spring 6+
- Easily applicable with @ExtendWith or with a service loader activation for the entire test suite
- Work with JReleaser to release the extension to Maven Central
- CI/CD with GitHub Actions
- in `demo` add real Spring Boot project to test the extension
