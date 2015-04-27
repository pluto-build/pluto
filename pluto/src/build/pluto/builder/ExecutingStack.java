package build.pluto.builder;

import java.util.Collection;
import java.util.List;

public class ExecutingStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Void> {

  protected Void cycleResult(BuildRequest<?, ?, ?, ?> cause, List<BuildRequest<?, ?, ?, ?>> scc) {
    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + cause.createBuilder().persistentPath(), cause, new BuildCycle(cause, scc));
    throw ex;
  }

  protected Void noCycleResult() {
    return null;
  }

  protected BuildRequest<?, ?, ?, ?> topMostEntry(Collection<BuildRequest<?, ?, ?, ?>> reqs) {
    for (BuildRequest<?, ?, ?, ?> r : this.callStack) {
      if (reqs.contains(r)) {
        return r;
      }
    }
    return null;
  }

}
