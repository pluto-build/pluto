package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;

public class ExecutingStack extends CycleDetectionStack<BuildUnit<?>, Void>{
  
  protected Void cycleResult(BuildUnit<?> unit, Set<BuildUnit<?>> scc) {
 // Get all elements of the scc
    List<BuildRequirement<?>>  cycleComponents = new ArrayList<>();
    for (BuildUnit<?> p : scc) {
      cycleComponents.add(requirementForEntry(p));
    }
    
    BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + unit.getPersistentPath(), unit, cycleComponents);
   throw ex;
  }
  protected Void noCycleResult() {
    return null;
  }
  
  
  private <Out extends Output> BuildRequirement<Out> requirementForEntry(BuildUnit<Out> unit) {
    return new BuildRequirement<>(unit, unit.getGeneratedBy());
  }

}
