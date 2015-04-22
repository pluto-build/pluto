package build.pluto.builder;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.BuildUnit;
import build.pluto.output.Output;

public abstract class BuildUnitProvider {

  public abstract
  //@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
 BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException;
  
  protected abstract Throwable tryCompileCycle(BuildCycleException e);
  
}
