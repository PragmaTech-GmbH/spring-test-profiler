# Configuration Reference

The Spring Test Profiler works with zero configuration. The options below let you adjust report output when the defaults do not fit your setup.

## System Properties

Pass these as JVM system properties to your test execution, for example via `-D` on the command line, the Surefire/Failsafe `argLine`, or the Gradle `test` task's `systemProperty`.

| Property | Default | Description |
|---|---|---|
| `pragmatech.spring.test.insight.report.dir` | _(unset)_ | Overrides the report output directory. When unset, the directory is derived from the detected build tool (see below). |
| `spring.test.insight.json.beta` | `false` | When `true`, additionally generates a JSON report next to the HTML report. This feature is in beta - see [Advanced Usage](Advanced-Usage.md#json-report-beta). |

### Examples

```bash
# Maven: custom report directory
./mvnw verify -Dpragmatech.spring.test.insight.report.dir=/tmp/profiler-reports

# Maven: enable the beta JSON report
./mvnw verify -Dspring.test.insight.json.beta=true
```

```groovy
// Gradle: build.gradle
test {
  systemProperty 'spring.test.insight.json.beta', 'true'
}
```

## Report Output

### Location

Unless overridden via `pragmatech.spring.test.insight.report.dir`, reports are written to a `spring-test-profiler` directory inside your build output folder:

| Build Tool | Report Directory |
|---|---|
| Maven | `target/spring-test-profiler/` |
| Gradle | `build/spring-test-profiler/` |
| Unknown | Falls back to detecting an existing `target/` or `build/` directory |

The build tool is detected automatically at runtime.

### File Naming

Each test run produces:

- `test-profiler-report-<timestamp>.html` - the timestamped report for this run
- `latest.html` - always points to the most recent report, so you can bookmark one stable path

Reports are self-contained HTML files with all CSS and JavaScript inlined - no external assets needed to view or archive them.

## Activation

Activation is not configured via properties but via Spring's `spring.factories` service loader mechanism or annotations - see [Getting Started](Getting-Started.md#2-activate-the-profiler).
