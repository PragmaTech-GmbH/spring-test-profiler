package digital.pragmatech.testing.optimization;

import java.util.List;

public interface TestSmellDetector {

  String getTestSmellType();

  String getDescription();

  List<OptimizationRecord> analyze(StaticAnalysisContext context);
}
