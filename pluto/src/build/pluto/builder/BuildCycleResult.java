package build.pluto.builder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BuildCycleResult {
  
  
  private Map<BuildRequest<?, ?, ?, ?>, Object> cycleOutputs = new HashMap<>();
  
  
  public <Out extends Serializable> void setBuildResult(BuildRequest<?, Out, ?, ?> req, Out result) {
    this.cycleOutputs.put(req, result);
  }
  
  @SuppressWarnings("unchecked")
  public <Out extends Serializable>  Out getResult(BuildRequest<?, Out, ?, ?> req) {
    return (Out) this.cycleOutputs.get(req);
  }
  
  
}