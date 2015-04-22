package build.pluto.builder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;

public class ExecutingStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Void> {

  protected Void cycleResult(BuildRequest<?, ?, ?, ?> req, Set<BuildRequest<?, ?, ?, ?>> scc) {
    // Get all elements of the scc
    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + req.createBuilder().persistentPath(), req, new BuildCycle(scc));
    throw ex;
  }

  protected Void noCycleResult() {
    return null;
  }

  private <Out extends Serializable> BuildRequirement<Out> requirementForEntry(BuildRequest<?, Out, ?, ?> req) throws IOException {
    return new BuildRequirement<Out>(BuildUnit.<Out> create(req.createBuilder().persistentPath(), req), req);
  }

}
