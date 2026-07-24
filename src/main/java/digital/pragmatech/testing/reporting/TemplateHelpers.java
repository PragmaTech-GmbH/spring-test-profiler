package digital.pragmatech.testing.reporting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.ContextIdGenerator;
import digital.pragmatech.testing.ContextLifespan;
import digital.pragmatech.testing.SpringContextStatistics;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;
import digital.pragmatech.testing.extensions.ContextCustomizerFormatter;
import digital.pragmatech.testing.util.SimpleJsonWriter;

/** Helper classes for Thymeleaf templates to format data and provide utility methods. */
public class TemplateHelpers {

  /** Static helper method to count tests by status from the execution tracker. */
  public static long countTestsByStatus(
      Map<String, TestExecutionTracker.TestClassMetrics> classMetrics, String statusName) {
    TestStatus status = TestStatus.valueOf(statusName);
    return classMetrics.values().stream()
        .flatMap(classMetric -> classMetric.getMethodMetrics().values().stream())
        .filter(methodMetric -> methodMetric.getStatus() == status)
        .count();
  }

  /** Instance helper class for counting test statuses (to be used in templates). */
  public static class TestStatusCounter {
    public long countTestsByStatus(
        Map<String, TestExecutionTracker.TestClassMetrics> classMetrics, String statusName) {
      return TemplateHelpers.countTestsByStatus(classMetrics, statusName);
    }
  }

  public static class DurationFormatter {
    public String format(long millis) {
      if (millis < 1000) {
        return millis + "ms";
      } else if (millis < 60000) {
        return String.format("%.1fs", millis / 1000.0);
      } else {
        return String.format("%.1fm", millis / 60000.0);
      }
    }
  }

  public static class ClassNameHelper {
    public String getSimpleClassName(String fullClassName) {
      int lastDot = fullClassName.lastIndexOf('.');
      return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }

    public String getPackageName(String fullClassName) {
      int lastDot = fullClassName.lastIndexOf('.');
      return lastDot >= 0 ? fullClassName.substring(0, lastDot) : "";
    }
  }

  public static class StatusColorHelper {
    public String getClassStatusColor(TestClassExecutionData classData) {
      if (classData.getFailedTests() > 0) {
        return "#e74c3c"; // red for any failures
      } else if (classData.getAbortedTests() > 0) {
        return "#f39c12"; // orange for aborted
      } else if (classData.getDisabledTests() > 0 && classData.getPassedTests() == 0) {
        return "#95a5a6"; // gray for all disabled
      } else {
        return "#27ae60"; // green for all passed
      }
    }
  }

  public static class StatusIconHelper {
    public String getStatusIcon(TestStatus status) {
      if (status == null) {
        return "❓"; // Unknown status icon
      }
      return switch (status) {
        case PASSED -> "✅";
        case FAILED -> "❌";
        case DISABLED -> "⏸️";
        case ABORTED -> "⚠️";
        case RUNNING -> "🔄";
        case PENDING -> "⏳";
      };
    }
  }

  public static class ErrorFormatter {
    public String formatError(Throwable throwable) {
      if (throwable == null) {
        return "";
      }

      StringBuilder sb = new StringBuilder();
      sb.append(throwable.getClass().getSimpleName()).append(": ");
      if (throwable.getMessage() != null) {
        sb.append(throwable.getMessage());
      }

      // Add first few stack trace lines for context
      StackTraceElement[] stackTrace = throwable.getStackTrace();
      if (stackTrace.length > 0) {
        sb.append("\n\nStack trace (first 5 lines):");
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
          sb.append("\n  at ").append(stackTrace[i].toString());
        }
        if (stackTrace.length > 5) {
          sb.append("\n  ... ").append(stackTrace.length - 5).append(" more");
        }
      }

      return sb.toString();
    }
  }

  public static class TestMethodSorter {
    public List<TestExecutionData> sortTestMethods(Collection<TestExecutionData> testMethods) {
      return testMethods.stream()
          .sorted(
              (a, b) -> {
                // Failed tests first
                if (a.getStatus() == TestStatus.FAILED && b.getStatus() != TestStatus.FAILED) {
                  return -1;
                }
                if (b.getStatus() == TestStatus.FAILED && a.getStatus() != TestStatus.FAILED) {
                  return 1;
                }
                // Then by name
                return a.getTestMethodName().compareTo(b.getTestMethodName());
              })
          .toList();
    }
  }

  public static class TestClassSorter {
    public List<TestClassExecutionData> sortTestClasses(List<TestClassExecutionData> testClasses) {
      return testClasses.stream().sorted((a, b) -> a.className().compareTo(b.className())).toList();
    }
  }

  public static class ClassNameComparator implements Comparator<TestClassExecutionData> {
    @Override
    public int compare(TestClassExecutionData a, TestClassExecutionData b) {
      return a.className().compareTo(b.className());
    }
  }

  public static class SummaryCalculator {
    public long getTotalTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream().mapToLong(TestClassExecutionData::getTotalTests).sum();
    }

    public long getPassedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream().mapToLong(TestClassExecutionData::getPassedTests).sum();
    }

    public long getFailedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream().mapToLong(TestClassExecutionData::getFailedTests).sum();
    }

    public long getDisabledTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream().mapToLong(TestClassExecutionData::getDisabledTests).sum();
    }

    public long getAbortedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream().mapToLong(TestClassExecutionData::getAbortedTests).sum();
    }

    public long getTotalExecutionTime(List<TestClassExecutionData> testClassData) {
      return testClassData.stream()
          .flatMap(classData -> classData.testExecutions().values().stream())
          .filter(testData -> testData.getDuration() != null)
          .mapToLong(testData -> testData.getDuration().toMillis())
          .sum();
    }

    public long getClassExecutionTime(TestClassExecutionData classData) {
      return classData.testExecutions().values().stream()
          .filter(testData -> testData.getDuration() != null)
          .mapToLong(testData -> testData.getDuration().toMillis())
          .sum();
    }
  }

  public static class ConfigurationHelper {
    private final ContextCacheTracker contextCacheTracker;

    public ConfigurationHelper(ContextCacheTracker contextCacheTracker) {
      this.contextCacheTracker = contextCacheTracker;
    }

    public Map<String, ContextConfigurationInfo> getConfigurations() {
      Map<String, ContextConfigurationInfo> configurations = new HashMap<>();

      if (contextCacheTracker != null) {
        // Convert ContextCacheTracker entries to the format expected by the template
        for (ContextCacheEntry entry : contextCacheTracker.getAllEntries()) {
          String configId =
              ContextIdGenerator.getContextId(entry.getConfiguration())
                  .replace("context-", "config-");

          ContextConfigurationInfo configInfo = new ContextConfigurationInfo();
          configInfo.id = configId;
          configInfo.testClasses = new HashSet<>(entry.getTestClasses());
          configInfo.configuration = entry.getConfigurationSummary();
          configInfo.primaryAnnotationType = entry.getPrimaryAnnotationType();

          configurations.put(configId, configInfo);
        }
      }
      return configurations;
    }

    // Helper class to match the structure expected by the template
    public static class ContextConfigurationInfo {
      public String id;
      public Set<String> testClasses;
      public Map<String, Object> configuration;
      public String primaryAnnotationType;
    }
  }

  public static class CacheKeyProcessor {
    public Map<String, Set<String>> aggregateCacheKeys(List<TestClassExecutionData> testClassData) {
      Map<String, Set<String>> allCacheKeys = new HashMap<>();

      for (TestClassExecutionData classData : testClassData) {
        SpringContextStatistics stats = classData.contextStatistics();
        if (stats != null) {
          Map<String, Set<String>> classCacheKeys = stats.getCacheKeyToTestClasses();
          for (Map.Entry<String, Set<String>> entry : classCacheKeys.entrySet()) {
            String cacheKey = entry.getKey();
            allCacheKeys.computeIfAbsent(cacheKey, k -> new HashSet<>()).add(classData.className());
          }
        }
      }

      return allCacheKeys;
    }

    public Map<String, SpringContextStatistics.CacheKeyInfo> aggregateCacheKeyInfo(
        List<TestClassExecutionData> testClassData) {
      Map<String, SpringContextStatistics.CacheKeyInfo> allCacheKeyInfo = new HashMap<>();

      for (TestClassExecutionData classData : testClassData) {
        SpringContextStatistics stats = classData.contextStatistics();
        if (stats != null) {
          Map<String, SpringContextStatistics.CacheKeyInfo> classCacheKeyInfo =
              stats.getCacheKeyInfoMap();
          for (Map.Entry<String, SpringContextStatistics.CacheKeyInfo> entry :
              classCacheKeyInfo.entrySet()) {
            String cacheKey = entry.getKey();
            SpringContextStatistics.CacheKeyInfo info = entry.getValue();
            SpringContextStatistics.CacheKeyInfo existing = allCacheKeyInfo.get(cacheKey);
            if (existing == null) {
              allCacheKeyInfo.put(cacheKey, new SpringContextStatistics.CacheKeyInfo(cacheKey));
              existing = allCacheKeyInfo.get(cacheKey);
            }
            // Aggregate the statistics
            for (int i = 0; i < info.getHits(); i++) {
              existing.incrementHits();
            }
            for (int i = 0; i < info.getMisses(); i++) {
              existing.incrementMisses();
            }
          }
        }
      }

      return allCacheKeyInfo;
    }
  }

  public static class JsonHelper {
    public String toJson(Object object) {
      try {
        return SimpleJsonWriter.toJson(object);
      } catch (Exception e) {
        return "[]";
      }
    }

    /**
     * Builds the JSON payload for the context cache timeline chart. All timestamps are epoch
     * milliseconds (SimpleJsonWriter serializes Instant with second granularity only, which is too
     * coarse for short test runs).
     */
    public String contextTimelineToJson(
        ContextCacheTracker contextCacheTracker,
        TestExecutionTracker executionTracker,
        int maxCacheSize) {
      if (contextCacheTracker == null) {
        return "{}";
      }

      List<Map<String, Object>> contexts = new ArrayList<>();
      long earliestSegmentStartMs = Long.MAX_VALUE;
      long latestSegmentEndMs = Long.MIN_VALUE;

      List<ContextCacheEntry> createdEntries =
          contextCacheTracker.getAllEntries().stream()
              .filter(ContextCacheEntry::isCreated)
              .filter(entry -> !entry.getLifespans().isEmpty())
              .sorted(
                  Comparator.comparing(
                      entry -> entry.getLifespans().get(0).getCreationTime(),
                      Comparator.nullsLast(Comparator.naturalOrder())))
              .toList();

      for (ContextCacheEntry entry : createdEntries) {
        List<Map<String, Object>> segments = new ArrayList<>();
        for (ContextLifespan lifespan : entry.getLifespans()) {
          if (lifespan.getCreationTime() == null) {
            continue;
          }
          long startMs = lifespan.getCreationTime().toEpochMilli();
          Long removedMs =
              lifespan.getRemovalTime() != null ? lifespan.getRemovalTime().toEpochMilli() : null;

          Map<String, Object> segment = new LinkedHashMap<>();
          segment.put("startMs", startMs);
          segment.put("loadMs", lifespan.getLoadTimeMs());
          segment.put("removedMs", removedMs);
          segment.put(
              "removalReason",
              lifespan.getRemovalReason() != null ? lifespan.getRemovalReason().name() : null);
          segments.add(segment);

          earliestSegmentStartMs = Math.min(earliestSegmentStartMs, startMs);
          latestSegmentEndMs =
              Math.max(latestSegmentEndMs, removedMs != null ? removedMs : startMs);
        }
        if (segments.isEmpty()) {
          continue;
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("contextKey", ContextIdGenerator.getContextId(entry.getConfiguration()));
        context.put("testClassCount", entry.getTestClasses().size());
        context.put("testClasses", new ArrayList<>(entry.getTestClasses()));
        context.put("beanCount", entry.getBeanDefinitionCount());
        context.put("segments", segments);
        context.put("testExecutions", buildTestExecutions(entry, executionTracker));
        contexts.add(context);
      }

      Instant overallStart =
          executionTracker != null ? executionTracker.getOverallStartTime() : null;
      Instant overallEnd = executionTracker != null ? executionTracker.getOverallEndTime() : null;

      Long testRunStartMs = overallStart != null ? overallStart.toEpochMilli() : null;
      if (earliestSegmentStartMs != Long.MAX_VALUE
          && (testRunStartMs == null || earliestSegmentStartMs < testRunStartMs)) {
        testRunStartMs = earliestSegmentStartMs;
      }

      Long testRunEndMs = overallEnd != null ? overallEnd.toEpochMilli() : null;
      if (testRunEndMs == null) {
        testRunEndMs =
            latestSegmentEndMs != Long.MIN_VALUE
                ? latestSegmentEndMs
                : Instant.now().toEpochMilli();
      }

      Map<String, Object> timeline = new LinkedHashMap<>();
      timeline.put("testRunStartMs", testRunStartMs);
      timeline.put("testRunEndMs", testRunEndMs);
      timeline.put("maxCacheSize", maxCacheSize);
      timeline.put("contexts", contexts);

      try {
        return SimpleJsonWriter.toJson(timeline);
      } catch (Exception e) {
        return "{}";
      }
    }

    /**
     * Collects the individual test method executions that ran on the given context, with epoch
     * millisecond timestamps matching the segment convention of the timeline payload.
     */
    private List<Map<String, Object>> buildTestExecutions(
        ContextCacheEntry entry, TestExecutionTracker executionTracker) {
      List<Map<String, Object>> testExecutions = new ArrayList<>();
      if (executionTracker == null) {
        return testExecutions;
      }

      Map<String, TestExecutionTracker.TestClassMetrics> classMetrics =
          executionTracker.getClassMetrics();
      for (String testMethodId : entry.getTestMethods()) {
        String[] parts = testMethodId.split("#");
        if (parts.length != 2) {
          continue;
        }
        TestExecutionTracker.TestClassMetrics classMetric = classMetrics.get(parts[0]);
        if (classMetric == null) {
          continue;
        }
        TestExecutionTracker.TestMethodMetrics methodMetric =
            classMetric.getMethodMetrics().get(parts[1]);
        if (methodMetric == null || methodMetric.getStartTime() == null) {
          continue;
        }

        long startMs = methodMetric.getStartTime().toEpochMilli();
        long endMs =
            methodMetric.getEndTime() != null ? methodMetric.getEndTime().toEpochMilli() : startMs;

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("testClass", parts[0]);
        execution.put("testMethod", parts[1]);
        execution.put("startMs", startMs);
        execution.put("endMs", endMs);
        execution.put(
            "status", methodMetric.getStatus() != null ? methodMetric.getStatus().name() : null);
        testExecutions.add(execution);
      }

      testExecutions.sort(Comparator.comparingLong(execution -> (long) execution.get("startMs")));
      return testExecutions;
    }

    public String contextStatisticsToJson(ContextCacheTracker contextCacheTracker) {
      if (contextCacheTracker == null) {
        return "[]";
      }

      List<Map<String, Object>> contextStatistics =
          contextCacheTracker.getAllEntries().stream()
              .filter(ContextCacheEntry::isCreated)
              .map(this::mapContextEntryToStatistics)
              .toList();

      return toJson(contextStatistics);
    }

    private Map<String, Object> mapContextEntryToStatistics(ContextCacheEntry entry) {
      Map<String, Object> statistics = new HashMap<>();

      // Generate unique context key using incrementing counter
      String contextKey = ContextIdGenerator.getContextId(entry.getConfiguration());
      statistics.put("contextKey", contextKey);

      // Load duration in milliseconds
      statistics.put("loadDuration", entry.getContextLoadTimeMs());

      // Unix UTC timestamps (convert Instant to epoch seconds)
      statistics.put(
          "initialLoadTime",
          entry.getCreationTime() != null ? entry.getCreationTime().getEpochSecond() : null);
      statistics.put(
          "lastUsedTime",
          entry.getLastUsedTime() != null ? entry.getLastUsedTime().getEpochSecond() : null);

      // Number of beans in the context
      statistics.put("numberOfBeans", entry.getBeanDefinitionCount());

      // Test classes using this context
      statistics.put("testClasses", new ArrayList<>(entry.getTestClasses()));

      // Test methods - need to enhance ContextCacheTracker to collect these
      statistics.put("testMethods", getTestMethodsForContext(entry));

      // Context configuration details
      statistics.put("contextConfiguration", mapContextConfiguration(entry));

      // Annotation type information for filtering
      statistics.put("testAnnotationTypes", new ArrayList<>(entry.getTestAnnotationTypes()));
      statistics.put("primaryAnnotationType", entry.getPrimaryAnnotationType());

      return statistics;
    }

    private List<String> getTestMethodsForContext(ContextCacheEntry entry) {
      return new ArrayList<>(entry.getTestMethods());
    }

    private Map<String, Object> mapContextConfiguration(ContextCacheEntry entry) {
      Map<String, Object> config = new HashMap<>();

      if (entry.getConfiguration() != null) {
        var mergedConfig = entry.getConfiguration();

        // Test class that defines this configuration
        config.put(
            "testClass",
            entry.getTestClasses().isEmpty() ? null : entry.getTestClasses().iterator().next());

        // Configuration locations
        config.put("locations", Arrays.asList(mergedConfig.getLocations()));

        // Configuration classes
        config.put(
            "classes", Arrays.stream(mergedConfig.getClasses()).map(Class::getName).toList());

        // Context initializer classes
        config.put(
            "contextInitializerClasses",
            mergedConfig.getContextInitializerClasses().stream().map(Class::getName).toList());

        // Active profiles
        config.put("activeProfiles", Arrays.asList(mergedConfig.getActiveProfiles()));

        // Property source locations
        config.put(
            "propertySourceLocations", Arrays.asList(mergedConfig.getPropertySourceLocations()));

        // Property source properties
        config.put(
            "propertySourceProperties", Arrays.asList(mergedConfig.getPropertySourceProperties()));

        // Context customizers
        config.put(
            "contextCustomizers",
            ContextCustomizerFormatter.formatAll(mergedConfig.getContextCustomizers()));

        // Context loader
        config.put(
            "contextLoader",
            mergedConfig.getContextLoader() != null
                ? mergedConfig.getContextLoader().getClass().getName()
                : null);

        // Parent context key if any
        config.put(
            "parent",
            mergedConfig.getParent() != null
                ? ContextIdGenerator.getContextId(mergedConfig.getParent())
                : null);
      }

      return config;
    }
  }

  /** Gets the context ID for a given configuration using the ContextIdGenerator. */
  public String getContextId(Object configuration) {
    if (configuration instanceof org.springframework.test.context.MergedContextConfiguration) {
      return ContextIdGenerator.getContextId(
          (org.springframework.test.context.MergedContextConfiguration) configuration);
    }
    return "context-unknown";
  }
}
