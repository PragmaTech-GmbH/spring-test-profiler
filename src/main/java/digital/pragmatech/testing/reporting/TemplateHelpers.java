package digital.pragmatech.testing.reporting;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import digital.pragmatech.testing.ContextCacheTracker;
import digital.pragmatech.testing.ContextCacheEntry;
import digital.pragmatech.testing.TimelineData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import digital.pragmatech.testing.SpringContextStatistics;
import digital.pragmatech.testing.TestExecutionTracker;
import digital.pragmatech.testing.TestStatus;

/**
 * Helper classes for Thymeleaf templates to format data and provide utility methods.
 */
public class TemplateHelpers {

  /**
   * Static helper method to count tests by status from the execution tracker.
   */
  public static long countTestsByStatus(Map<String, TestExecutionTracker.TestClassMetrics> classMetrics, String statusName) {
    TestStatus status = TestStatus.valueOf(statusName);
    return classMetrics.values().stream()
      .flatMap(classMetric -> classMetric.getMethodMetrics().values().stream())
      .filter(methodMetric -> methodMetric.getStatus() == status)
      .count();
  }

  /**
   * Instance helper class for counting test statuses (to be used in templates).
   */
  public static class TestStatusCounter {
    public long countTestsByStatus(Map<String, TestExecutionTracker.TestClassMetrics> classMetrics, String statusName) {
      return TemplateHelpers.countTestsByStatus(classMetrics, statusName);
    }
  }

  public static class DurationFormatter {
    public String format(long millis) {
      if (millis < 1000) {
        return millis + "ms";
      }
      else if (millis < 60000) {
        return String.format("%.1fs", millis / 1000.0);
      }
      else {
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
      }
      else if (classData.getAbortedTests() > 0) {
        return "#f39c12"; // orange for aborted
      }
      else if (classData.getDisabledTests() > 0 && classData.getPassedTests() == 0) {
        return "#95a5a6"; // gray for all disabled
      }
      else {
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
        .sorted((a, b) -> {
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
        .collect(Collectors.toList());
    }
  }

  public static class TestClassSorter {
    public List<TestClassExecutionData> sortTestClasses(List<TestClassExecutionData> testClasses) {
      return testClasses.stream()
        .sorted((a, b) -> a.className().compareTo(b.className()))
        .collect(Collectors.toList());
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
      return testClassData.stream()
        .mapToLong(TestClassExecutionData::getTotalTests)
        .sum();
    }

    public long getPassedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream()
        .mapToLong(TestClassExecutionData::getPassedTests)
        .sum();
    }

    public long getFailedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream()
        .mapToLong(TestClassExecutionData::getFailedTests)
        .sum();
    }

    public long getDisabledTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream()
        .mapToLong(TestClassExecutionData::getDisabledTests)
        .sum();
    }

    public long getAbortedTests(List<TestClassExecutionData> testClassData) {
      return testClassData.stream()
        .mapToLong(TestClassExecutionData::getAbortedTests)
        .sum();
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
          String configId = "config-" + Math.abs(entry.getConfiguration().hashCode());

          ContextConfigurationInfo configInfo = new ContextConfigurationInfo();
          configInfo.id = configId;
          configInfo.testClasses = new HashSet<>(entry.getTestClasses());
          configInfo.configuration = entry.getConfigurationSummary();

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

    public Map<String, SpringContextStatistics.CacheKeyInfo> aggregateCacheKeyInfo(List<TestClassExecutionData> testClassData) {
      Map<String, SpringContextStatistics.CacheKeyInfo> allCacheKeyInfo = new HashMap<>();

      for (TestClassExecutionData classData : testClassData) {
        SpringContextStatistics stats = classData.contextStatistics();
        if (stats != null) {
          Map<String, SpringContextStatistics.CacheKeyInfo> classCacheKeyInfo = stats.getCacheKeyInfoMap();
          for (Map.Entry<String, SpringContextStatistics.CacheKeyInfo> entry : classCacheKeyInfo.entrySet()) {
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
    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      return mapper;
    }

    public String toJson(Object object) {
      try {
        return objectMapper.writeValueAsString(object);
      } catch (Exception e) {
        return "[]";
      }
    }

    public String timelineEventsToJson(TimelineData timelineData) {
      if (timelineData == null || timelineData.events() == null) {
        return "[]";
      }
      return toJson(timelineData.events());
    }
  }
}
