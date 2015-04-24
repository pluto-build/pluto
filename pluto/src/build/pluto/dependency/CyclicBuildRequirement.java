package build.pluto.dependency;

import java.io.IOException;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.output.Output;
import build.pluto.output.OutputStamp;

public class CyclicBuildRequirement<Out extends Output> extends BuildRequirement<Out> {
  
  public CyclicBuildRequirement() { /* for deserialization */ }
  
  public CyclicBuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req, Out output) {
    super(unit, req, req.stamper.stampOf(output));
  }

  @Override
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException {
    boolean hasFailed = isHasFailed();
    BuildUnit<Out> unit = getUnit();
    OutputStamp<? super Out> stamp = getStamp();
    BuildRequest<?, Out, ?, ?> req = getRequest();
    boolean wasFailed = hasFailed || unit != null && unit.hasFailed();
    BuildUnit<Out> newUnit = manager.require(req).getUnit();
    hasFailed = newUnit.hasFailed();

    if (wasFailed && !hasFailed)
      return false;

    boolean stampOK = stamp == null || stamp.equals(stamp.getStamper().stampOf(newUnit.getBuildResult()));
    // if (!stampOK)
    // return false;

    return true;

  }
}
