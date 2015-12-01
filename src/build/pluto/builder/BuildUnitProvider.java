package build.pluto.builder;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;
import build.pluto.util.IReporting;

public abstract class BuildUnitProvider {

  protected final IReporting report;
  
  public BuildUnitProvider(IReporting report) {
    this.report = report;
  }
  
  public abstract
  //@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
 BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq, boolean needBuildResult) throws IOException;

  
  public
  //@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    return require(buildReq, true);
  }

  protected abstract Throwable tryCompileCycle(BuildCycleException e);

}
