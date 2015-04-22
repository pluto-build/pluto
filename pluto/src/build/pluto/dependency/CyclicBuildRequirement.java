package build.pluto.dependency;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.output.Output;

public class CyclicBuildRequirement<OutT extends Output> extends BuildRequirement<OutT> {
  
  public CyclicBuildRequirement(BuildUnit<OutT> unit, BuildRequest<?, OutT, ?, ?> req, OutT output) {
    super(unit, req, req.stamper.stampOf(output));
  }
}
