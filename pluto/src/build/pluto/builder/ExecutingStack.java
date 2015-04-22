package build.pluto.builder;

import java.util.Set;

public class ExecutingStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Void> {

  protected Void cycleResult(BuildRequest<?, ?, ?, ?> req, Set<BuildRequest<?, ?, ?, ?>> scc) {
    // Get all elements of the scc
    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + req.createBuilder().persistentPath(), req, new BuildCycle(scc));
    throw ex;
  }

  protected Void noCycleResult() {
    return null;
  }

//  private <Out extends Output> BuildRequirement<Out> requirementForEntry(BuildRequest<?, Out, ?, ?> req) throws IOException {
//    return new BuildRequirement<Out>(BuildUnit.<Out> create(req.createBuilder().persistentPath(), req), req);
//  }

}
