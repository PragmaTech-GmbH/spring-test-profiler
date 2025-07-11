# Spring Test Insight

A JUnit Jupiter extension that provides visualization and insights for Spring Test execution, with a focus on Spring context caching statistics.

## Features

- Visualize test results in a clean HTML report
- Track Spring Test context caching statistics
- Show context reuse metrics and cache hit/miss ratios
- Identify tests that couldn't reuse contexts and explain why
- Support for Spring 6+ and Java 21+
- Easy integration via `@ExtendWith` or automatic service loader activation
- Works with both Maven Surefire/Failsafe and Gradle test tasks

## Usage

### Quick Start

Add the dependency to your project:

```xml
<dependency>
    <groupId>digital.pragmatech</groupId>
    <artifactId>spring-test-profiler-extension</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### Automatic Activation (Recommended)

The extension automatically activates for all tests via service loader. No additional configuration needed.

### Manual Activation

Alternatively, add the extension to specific test classes:

```java
@ExtendWith(SpringTestInsightExtension.class)
@SpringBootTest
class MySpringTest {
    // Your tests here
}
```

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

### Demo Project

See the `demo` directory for a complete Spring Boot example showcasing the extension.

```bash
# Run the demo
cd demo
mvn clean test
# Open target/spring-test-profiler/latest.html
```

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
   ./mvnw clean install
   ```
3. **Run tests**:
   ```bash
   ./mvnw test
   ```

### Making Changes

1. **Create a feature branch** from `main`
2. **Make your changes** following the existing code style
3. **Add tests** for new functionality
4. **Ensure all tests pass**:
   ```bash
   ./mvnw clean verify
   ```
5. **Test with the demo**:
   ```bash
   cd demo
   mvn clean test
   ```

### Submitting Changes

1. **Commit your changes** with clear, descriptive messages
2. **Push to your fork** and create a pull request
3. **Describe your changes** in the PR description
4. **Wait for review** and address any feedback

### Code Style

- Follow existing Java conventions
- Use meaningful variable and method names
- Add JavaDoc for public APIs
- Keep methods focused and concise

### Areas for Contribution

- Additional report formats (JSON, XML)
- Integration with other testing frameworks
- Performance optimizations
- Documentation improvements
- Bug fixes and testing

## Requirements

- Java 21+
- Spring 6+
- JUnit Jupiter 5.8+
