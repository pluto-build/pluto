package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.common.path.Path;

public class FixpointCycleBuildResultProvider implements BuildUnitProvider{
  
  private BuildUnitProvider parentManager;
  
  private BuildCycle cycle;

  
  
  public FixpointCycleBuildResultProvider(BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
  }

  private
    <In extends Serializable,
     Out extends BuildOutput, 
     B extends Builder<In, Out>, 
     F extends BuilderFactory<In, Out, B>> 
  BuildUnit<Out> getBuildUnitInCycle(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    Path depPath = buildReq.createBuilder().persistentPath();
    for (BuildRequirement<?> req : this.cycle.getCycleComponents()) {
      if (req.unit.getPersistentPath().equals(depPath)) {
        return  (BuildUnit<Out>) req.unit;
      }
    }
    return null;
  }
  
  @Override
  public 
    <In extends Serializable,
     Out extends BuildOutput, 
     B extends Builder<In, Out>, 
     F extends BuilderFactory<In, Out, B>> 
   BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    BuildUnit<Out> cycleUnit = getBuildUnitInCycle(buildReq);
    if (cycleUnit != null) {
      return cycleUnit;
    } else {
      return this.parentManager.require(buildReq);
    }
  }
  
  

}
