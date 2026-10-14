package digital.pragmatech.testing;

import java.util.List;

/**
 * Immutable snapshot of the Spring context cache statistics of the current JVM.
 *
 * <p>Use {@link SpringTestProfilerListener#getContextCacheStatistics()} to get a snapshot, for
 * example to fail the build when too many contexts were created. The values are only final after
 * all tests in the JVM have run. Every test framework can use this class, because it does not
 * depend on JUnit.
 *
 * @param contextsCreated number of context creations, including re-creations after
 *     {@code @DirtiesContext}
 * @param distinctContextsCreated number of distinct context configurations created at least once
 * @param cacheHits number of context cache hits
 * @param cacheMisses number of context cache misses
 * @param cacheHitRatio cache hits divided by all cache accesses (0.0 to 1.0)
 * @param totalContextCreationTimeMs sum of all context load times
 * @param wastedContextTimeMs context load time that could be saved by reusing contexts
 * @param contexts the distinct contexts in creation order
 */
public record ContextCacheStatistics(
    int contextsCreated,
    int distinctContextsCreated,
    int cacheHits,
    int cacheMisses,
    double cacheHitRatio,
    long totalContextCreationTimeMs,
    long wastedContextTimeMs,
    List<ContextStatistics> contexts) {

  /**
   * One distinct context configuration.
   *
   * @param testClasses the test classes that use this context, sorted by name
   * @param loadTimeMs the load time of the context
   * @param beanDefinitionCount number of bean definitions in the context
   */
  public record ContextStatistics(
      List<String> testClasses, long loadTimeMs, int beanDefinitionCount) {}

  /** Returns a readable multi-line list of the contexts and their test classes. */
  public String describeContexts() {
    StringBuilder description = new StringBuilder();
    int contextNumber = 1;
    for (ContextStatistics context : contexts) {
      description.append("\n  ").append(contextNumber++).append(". ").append(context.testClasses());
    }
    return description.toString();
  }
}
