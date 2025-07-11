
package digital.pragmatech.springtestinsight.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import digital.pragmatech.springtestinsight.ContextCacheTracker;
import digital.pragmatech.springtestinsight.SpringContextCacheAccessor;
import digital.pragmatech.springtestinsight.TestExecutionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class JsonReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JsonReportGenerator.class);

    private final ObjectMapper objectMapper;

    public JsonReportGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void generateJsonReport(Path reportDir, String phase, TestExecutionTracker executionTracker,
                                   SpringContextCacheAccessor.CacheStatistics cacheStats,
                                   ContextCacheTracker contextCacheTracker) {
        try {
            Files.createDirectories(reportDir);

            String uniqueId = UUID.randomUUID().toString();
            String jsonFileName = String.format("spring-test-profiler-%s-%s.json", phase, uniqueId);
            Path jsonFile = reportDir.resolve(jsonFileName);

            ReportData reportData = new ReportData(phase, executionTracker, cacheStats, contextCacheTracker);

            objectMapper.writeValue(jsonFile.toFile(), reportData);

            logger.info("Successfully generated JSON report: {}", jsonFile.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to generate JSON report", e);
        }
    }

    // A simple data holder class for serialization
    private static class ReportData {
        private final String phase;
        private final TestExecutionTracker executionTracker;
        private final SpringContextCacheAccessor.CacheStatistics cacheStats;
        private final ContextCacheTracker contextCacheTracker;

        public ReportData(String phase, TestExecutionTracker executionTracker,
                          SpringContextCacheAccessor.CacheStatistics cacheStats,
                          ContextCacheTracker contextCacheTracker) {
            this.phase = phase;
            this.executionTracker = executionTracker;
            this.cacheStats = cacheStats;
            this.contextCacheTracker = contextCacheTracker;
        }

        // Getters are needed by Jackson for serialization
        public String getPhase() {
            return phase;
        }

        public TestExecutionTracker getExecutionTracker() {
            return executionTracker;
        }

        public SpringContextCacheAccessor.CacheStatistics getCacheStats() {
            return cacheStats;
        }

        public ContextCacheTracker getContextCacheTracker() {
            return contextCacheTracker;
        }
    }
}
