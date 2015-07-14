package build.pluto.builder;

import java.util.List;

import org.sugarj.common.Log;

public class ExecutingStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Void> {

  protected Void cycleResult(BuildRequest<?, ?, ?, ?> cause, List<BuildRequest<?, ?, ?, ?>> scc) {
    BuildRequest<?, ?, ?, ?> topRequest = topMostEntry(scc);

    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + topRequest.createBuilder().persistentPath(), topRequest, new BuildCycle(topRequest, scc));
    throw ex;
  }

  protected Void noCycleResult() {
    return null;
  }


}
