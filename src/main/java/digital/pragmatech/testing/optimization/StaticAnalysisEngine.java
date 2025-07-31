package digital.pragmatech.testing.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticAnalysisEngine {

  private static final Logger logger = LoggerFactory.getLogger(StaticAnalysisEngine.class);

  private final List<TestSmellDetector> detectors;

  public StaticAnalysisEngine() {
    this.detectors = loadDetectors();
  }

  public StaticAnalysisEngine(List<TestSmellDetector> detectors) {
    this.detectors = new ArrayList<>(detectors);
  }

  public List<OptimizationRecord> analyze(StaticAnalysisContext context) {
    List<OptimizationRecord> allOptimizations = new ArrayList<>();

    for (TestSmellDetector detector : detectors) {
      try {
        logger.debug("Running detector: {}", detector.getTestSmellType());
        List<OptimizationRecord> optimizations = detector.analyze(context);
        allOptimizations.addAll(optimizations);
        logger.debug(
            "Detector {} found {} optimization opportunities",
            detector.getTestSmellType(),
            optimizations.size());
      } catch (Exception e) {
        logger.warn(
            "Error running detector {}: {}", detector.getTestSmellType(), e.getMessage(), e);
      }
    }

    logger.info(
        "Static analysis completed. Found {} optimization opportunities", allOptimizations.size());
    return allOptimizations;
  }

  public List<TestSmellDetector> getDetectors() {
    return new ArrayList<>(detectors);
  }

  private List<TestSmellDetector> loadDetectors() {
    List<TestSmellDetector> loadedDetectors = new ArrayList<>();

    // Load detectors via ServiceLoader
    ServiceLoader<TestSmellDetector> serviceLoader = ServiceLoader.load(TestSmellDetector.class);
    for (TestSmellDetector detector : serviceLoader) {
      loadedDetectors.add(detector);
      logger.debug("Loaded detector: {}", detector.getTestSmellType());
    }

    // If no detectors found via ServiceLoader, add default ones
    if (loadedDetectors.isEmpty()) {
      logger.debug("No detectors found via ServiceLoader, adding default detectors");
      loadedDetectors.add(new DirtiesContextTestSmellDetector());
    }

    return loadedDetectors;
  }
}
