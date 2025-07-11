package digital.pragmatech.springtestinsight.experiment;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.EngineDiscoveryResult;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

public class MyDiscoveryListener implements LauncherDiscoveryListener {

  @Override
  public void engineDiscoveryFinished(UniqueId engineId, EngineDiscoveryResult result) {
    LauncherDiscoveryListener.super.engineDiscoveryFinished(engineId, result);
  }

  @Override
  public void engineDiscoveryStarted(UniqueId engineId) {
    LauncherDiscoveryListener.super.engineDiscoveryStarted(engineId);
    System.out.println(engineId.getEngineId());

  }

  @Override
  public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
    LauncherDiscoveryListener.super.launcherDiscoveryFinished(request);

    System.out.println(request.getOutputDirectoryProvider());

  }

  @Override
  public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
    LauncherDiscoveryListener.super.launcherDiscoveryStarted(request);

    System.out.println(request.getOutputDirectoryProvider());
  }

}
