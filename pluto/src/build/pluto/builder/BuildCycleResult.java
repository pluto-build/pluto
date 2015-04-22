package build.pluto.builder;

import java.util.HashMap;
import java.util.Map;

import build.pluto.output.Output;

public class BuildCycleResult {
  
  
  private Map<BuildRequest<?, ?, ?, ?>, Object> cycleOutputs = new HashMap<>();
  
  
  public <Out extends Output> void setBuildResult(BuildRequest<?, Out, ?, ?> req, Out result) {
    this.cycleOutputs.put(req, result);
  }
  
  @SuppressWarnings("unchecked")
  public <Out extends Output>  Out getResult(BuildRequest<?, Out, ?, ?> req) {
    return (Out) this.cycleOutputs.get(req);
  }
  
  
}