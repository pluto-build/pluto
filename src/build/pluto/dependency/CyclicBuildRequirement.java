package build.pluto.dependency;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.output.Output;

public class CyclicBuildRequirement<Out extends Output> extends BuildRequirement<Out> {
  
  public CyclicBuildRequirement() { /* for deserialization */ }
  
  public CyclicBuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req, Out output) {
    super(unit, req, req.stamper.stampOf(output));
  }

}
