package build.pluto.builder;

import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.util.Pair;

import build.pluto.BuildUnit;
import build.pluto.output.Output;

public class BuildCycleResult {
  
  
  private Map<BuildRequest<?, ?, ?, ?>, UnitOutputPair<?>> cycleOutputs = new HashMap<>();
  
  public static class UnitOutputPair<Out extends Output> extends Pair<BuildUnit<Out>, Out> {

    public UnitOutputPair(BuildUnit<Out> a, Out b) {
      super(a, b);
    }

    public void setOutput() {
      a.setBuildResult(b);
    }

  }
  
  public <Out extends Output> void setBuildResult(BuildRequest<?, Out, ?, ?> req, Out result, BuildUnit<Out> unit) {
    this.cycleOutputs.put(req, new UnitOutputPair<>(unit, result));
  }
  
  @SuppressWarnings("unchecked")
  public <Out extends Output> UnitOutputPair<Out> getResult(BuildRequest<?, Out, ?, ?> req) {
    return (UnitOutputPair<Out>) this.cycleOutputs.get(req);
  }
  
  
}