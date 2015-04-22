package build.pluto.builder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import build.pluto.BuildUnit;

public class BuildCycleResult {
  
  public static class  UnitResultTuple <Out extends Serializable>{
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
  
  private Map<BuildUnit<?>, BuildCycleResult.UnitResultTuple<?>> cycleOutputs = new HashMap<>();
  
  
  public <Out extends Serializable> void setBuildResult(BuildUnit<Out> unit, Out result) {
    this.cycleOutputs.put(unit, new BuildCycleResult.UnitResultTuple<Out>(unit, result));
  }
  
  public <Out extends Serializable>  BuildCycleResult.UnitResultTuple<Out> getUnitResult(BuildUnit<Out> unit) {
    @SuppressWarnings("unchecked")
    BuildCycleResult.UnitResultTuple<Out> tuple = (BuildCycleResult.UnitResultTuple<Out>) this.cycleOutputs.get(unit);
    return tuple;
  }
  
  
}