# Contributing

We welcome contributions! This page covers everything you need to get a local development environment running.

## Development Setup

1. **Fork and clone** the [repository](https://github.com/PragmaTech-GmbH/spring-test-profiler)
2. **Activate pre-commit hooks** (this ensures compliant code formatting): `pre-commit install` ([pre-commit download](https://pre-commit.com/))
3. **Build the project**:

```bash
./mvnw install
```

4. **Run the tests**:

```bash
./mvnw test
```

## Testing Your Changes Against the Demos

The [demo projects](Demo-Projects.md) are the quickest way to see your change in a realistic setup:

```bash
./mvnw clean install
cd demo/spring-boot-3.5-maven
mvn clean verify
# Inspect target/spring-test-profiler/latest.html
```

## Commit Messages

Use conventional commit messages for your changes, for example:

- `feat: add new feature`
- `fix: resolve issue #123`
- `docs: clarify Gradle setup`

## Documentation

The GitHub wiki is generated from the [`docs/wiki/`](https://github.com/PragmaTech-GmbH/spring-test-profiler/tree/main/docs/wiki) directory in the repository - it is the single source of truth for documentation. To improve the docs, edit the files there and open a pull request. Do **not** edit wiki pages through the GitHub wiki UI: changes made there are overwritten on the next sync.

## Reporting Bugs

See [Troubleshooting and Limitations](Troubleshooting-and-Limitations.md#reporting-bugs).
