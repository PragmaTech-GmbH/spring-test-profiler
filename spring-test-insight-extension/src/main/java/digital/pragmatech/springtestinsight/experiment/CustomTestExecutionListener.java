package digital.pragmatech.springtestinsight.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class CustomTestExecutionListener extends AbstractTestExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(CustomTestExecutionListener.class);

  @Override
  public int getOrder() {
    return super.getOrder();
  }

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    LOG.info("Before test class");
    LOG.info("Test application context id: {}", testContext.getApplicationContext().getId());
    LOG.info("Test class: {}", testContext.getTestClass().getName());

    var contextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper( testContext.getTestClass());
    var delegate = (DefaultCacheAwareContextLoaderDelegate) contextBootstrapper.getBootstrapContext().getCacheAwareContextLoaderDelegate();


    MergedContextConfiguration contextCache = contextBootstrapper.buildMergedContextConfiguration();
    LOG.info("Bootstrapper: {}", contextBootstrapper);
    LOG.info("Bootstrapper: {}", contextBootstrapper.getBootstrapContext().getCacheAwareContextLoaderDelegate().isContextLoaded(contextCache));
    LOG.info("Bootstrapper: {}", contextCache);
    LOG.info("Bootstrapper: {}", contextCache.hashCode());
  }

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    LOG.info("Prepare test class");
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    LOG.info("Before test method: {}", testContext.getTestMethod().getName());
  }

  @Override
  public void beforeTestExecution(TestContext testContext) throws Exception {
    LOG.info("Before test execution: {}", testContext.getTestMethod().getName());
  }

  @Override
  public void afterTestExecution(TestContext testContext) throws Exception {
    LOG.info("After test execution: {}", testContext.getTestMethod().getName());
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    LOG.info("After test execution: {}", testContext.getTestMethod().getName());
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    LOG.info("After test class: {}", testContext.getApplicationContext().getId());
  }
}
