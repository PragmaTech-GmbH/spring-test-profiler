package digital.pragmatech.springtestinsight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ContextConfigurationDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextConfigurationDetector.class);
    private static final Map<MergedContextConfiguration, Set<String>> configToTestClasses = new ConcurrentHashMap<>();
    private static final Map<String, MergedContextConfiguration> testClassToConfig = new ConcurrentHashMap<>();
    
    /**
     * Analyzes a test class and extracts its MergedContextConfiguration.
     * This method uses Spring's internal APIs to determine the context configuration
     * that would be used for the test class.
     */
    public static void analyzeTestClass(Class<?> testClass) {
        try {
            // In Spring 6, resolveTestContextBootstrapper takes a Class parameter directly
            TestContextBootstrapper bootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
            
            // Build the merged context configuration
            MergedContextConfiguration mergedConfig = bootstrapper.buildMergedContextConfiguration();
            
            // Store the mapping
            String className = testClass.getName();
            testClassToConfig.put(className, mergedConfig);
            configToTestClasses.computeIfAbsent(mergedConfig, k -> ConcurrentHashMap.newKeySet()).add(className);
            
            logger.info("Analyzed test class {} with configuration: {}", className, getConfigurationSummary(mergedConfig));
        } catch (Exception e) {
            logger.warn("Failed to analyze test class {}: {}", testClass.getName(), e.getMessage());
        }
    }
    
    /**
     * Returns all discovered context configurations with their associated test classes.
     */
    public static Map<String, ContextConfigurationInfo> getContextConfigurations() {
        logger.info("Getting context configurations. Total configs detected: {}, Test class mappings: {}", 
            configToTestClasses.size(), testClassToConfig.size());
        Map<String, ContextConfigurationInfo> result = new LinkedHashMap<>();
        
        int configId = 1;
        for (Map.Entry<MergedContextConfiguration, Set<String>> entry : configToTestClasses.entrySet()) {
            MergedContextConfiguration config = entry.getKey();
            Set<String> testClasses = entry.getValue();
            
            String configKey = "context-" + configId++;
            ContextConfigurationInfo info = new ContextConfigurationInfo(
                configKey,
                getConfigurationDetails(config),
                new ArrayList<>(testClasses)
            );
            
            result.put(configKey, info);
        }
        
        return result;
    }
    
    /**
     * Gets the context configuration for a specific test class.
     */
    public static Optional<MergedContextConfiguration> getConfigurationForTestClass(String testClassName) {
        return Optional.ofNullable(testClassToConfig.get(testClassName));
    }
    
    /**
     * Clears all stored configuration data.
     */
    public static void clear() {
        logger.info("Clearing context configuration data. Had {} configs and {} test class mappings", 
            configToTestClasses.size(), testClassToConfig.size());
        configToTestClasses.clear();
        testClassToConfig.clear();
    }
    
    private static String getConfigurationSummary(MergedContextConfiguration config) {
        return String.format("locations=%s, classes=%s, contextLoader=%s",
            Arrays.toString(config.getLocations()),
            Arrays.toString(config.getClasses()),
            config.getContextLoader() != null ? config.getContextLoader().getClass().getSimpleName() : "null"
        );
    }
    
    private static Map<String, Object> getConfigurationDetails(MergedContextConfiguration config) {
        Map<String, Object> details = new LinkedHashMap<>();
        
        // Basic configuration
        details.put("locations", Arrays.asList(config.getLocations()));
        details.put("classes", Arrays.stream(config.getClasses())
            .map(Class::getName)
            .collect(Collectors.toList()));
        
        // Context loader
        if (config.getContextLoader() != null) {
            details.put("contextLoader", config.getContextLoader().getClass().getName());
        }
        
        // Properties
        if (config.getPropertySourceLocations() != null && config.getPropertySourceLocations().length > 0) {
            details.put("propertySourceLocations", Arrays.asList(config.getPropertySourceLocations()));
        }
        
        if (config.getPropertySourceProperties() != null && config.getPropertySourceProperties().length > 0) {
            details.put("propertySourceProperties", Arrays.asList(config.getPropertySourceProperties()));
        }
        
        // Active profiles
        if (config.getActiveProfiles() != null && config.getActiveProfiles().length > 0) {
            details.put("activeProfiles", Arrays.asList(config.getActiveProfiles()));
        }
        
        // Context initializers
        if (config.getContextInitializerClasses() != null && !config.getContextInitializerClasses().isEmpty()) {
            details.put("contextInitializers", config.getContextInitializerClasses().stream()
                .map(Class::getName)
                .collect(Collectors.toList()));
        }
        
        // Context customizers
        if (config.getContextCustomizers() != null && !config.getContextCustomizers().isEmpty()) {
            details.put("contextCustomizers", config.getContextCustomizers().stream()
                .map(customizer -> customizer.getClass().getName())
                .collect(Collectors.toList()));
        }
        
        // Parent configuration
        if (config.getParent() != null) {
            details.put("hasParentConfiguration", true);
        }
        
        return details;
    }
    
    /**
     * Data class to hold context configuration information.
     */
    public static class ContextConfigurationInfo {
        private final String id;
        private final Map<String, Object> configuration;
        private final List<String> testClasses;
        
        public ContextConfigurationInfo(String id, Map<String, Object> configuration, List<String> testClasses) {
            this.id = id;
            this.configuration = configuration;
            this.testClasses = testClasses;
        }
        
        public String getId() {
            return id;
        }
        
        public Map<String, Object> getConfiguration() {
            return configuration;
        }
        
        public List<String> getTestClasses() {
            return testClasses;
        }
    }
}