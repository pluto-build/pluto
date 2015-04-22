package build.pluto.builder;

import java.util.HashSet;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;

public class ExecutingStack extends CycleDetectionStack<BuildUnit<?>, Void> {

  protected Void cycleResult(BuildUnit<?> cause, Set<BuildUnit<?>> scc) {
    // Get all elements of the scc
    Set<BuildRequirement<?>>  cycleComponents = new HashSet<>();
    for (BuildUnit<?> sccUnit : scc) {
      cycleComponents.add(requirementForEntry(sccUnit));
    }
    
    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + cause.getPersistentPath(), cause, new BuildCycle(cycleComponents));
    throw ex;
  }

  protected Void noCycleResult() {
    return null;
  }

  private <Out extends Output> BuildRequirement<Out> requirementForEntry(BuildUnit<Out> unit) {
    return new BuildRequirement<Out>(unit, unit.getGeneratedBy());
  }

}
