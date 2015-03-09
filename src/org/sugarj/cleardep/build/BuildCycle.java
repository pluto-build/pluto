package org.sugarj.cleardep.build;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.common.path.Path;

public class BuildCycle {
  
  public static class Result {
    
    public static class  UnitResultTuple <Out extends BuildOutput>{
      private BuildUnit<Out> unit;
      private Out result;
      public UnitResultTuple(BuildUnit<Out> unit, Out result) {
        super();
        this.unit = unit;
        this.result = result;
      }
      public void setOutputToUnit() {
        unit.setBuildResult(result);
      }
    }
    
    private Map<Path, UnitResultTuple<?>> cycleOutpus = new HashMap<>();
    
    
    public <Out extends BuildOutput> void setBuildResult(BuildUnit<Out> unit, Out result) {
      this.cycleOutpus.put(unit.getPersistentPath(), new UnitResultTuple<Out>(unit, result));
    }
    
    public <Out extends BuildOutput>  UnitResultTuple<Out> getUnitResult(BuildUnit<Out> unit) {
      UnitResultTuple<Out> tuple = (UnitResultTuple<Out>) this.cycleOutpus.get(unit.getPersistentPath());
      return tuple;
    }
    
    
  }
  
  private List<BuildRequirement<?>> cycleComponents;

  public BuildCycle(List<BuildRequirement<?>> cycleComponents) {
    super();
    this.cycleComponents = cycleComponents;
  }
  
  public List<BuildRequirement<?>> getCycleComponents() {
    return cycleComponents;
  }

  // TODO get edited source files
  public boolean isConsistent() {
    for (BuildRequirement<?> req : this.cycleComponents){
      if (!req.unit.isConsistent(null))
        return false;
    }
    return true;
  }
  
  public BuildRequirement<?> getInitialComponent() {
    return this.cycleComponents.get(0);
  }
  
}
