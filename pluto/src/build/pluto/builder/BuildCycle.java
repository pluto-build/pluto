package build.pluto.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;
import build.pluto.util.AbsoluteComparedFile;

public class BuildCycle {
  
  public static class Result {
    
    public static class  UnitResultTuple <Out extends Output>{
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
    
    private Map<AbsoluteComparedFile, UnitResultTuple<?>> cycleOutputs = new HashMap<>();
    
    
    public <Out extends Output> void setBuildResult(BuildUnit<Out> unit, Out result) {
      this.cycleOutputs.put(AbsoluteComparedFile.absolute(unit.getPersistentPath()), new UnitResultTuple<Out>(unit, result));
    }
    
    public <Out extends Output>  UnitResultTuple<Out> getUnitResult(BuildUnit<Out> unit) {
      UnitResultTuple<Out> tuple = (UnitResultTuple<Out>) this.cycleOutputs.get(AbsoluteComparedFile.absolute(unit.getPersistentPath()));
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


  protected CycleSupport getCycleSupport() {
    for (BuildRequirement<?> requirement : this.getCycleComponents()) {
      CycleSupport support = requirement.getRequest().createBuilder().getCycleSupport();
      if (support != null && support.canCompileCycle(this)) {
        return support;
      }
    }
    return null;
  }
  
  public BuildRequirement<?> getInitialComponent() {
    return this.cycleComponents.get(0);
  }
  
}
