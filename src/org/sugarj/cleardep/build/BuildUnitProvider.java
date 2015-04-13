package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;

public abstract class BuildUnitProvider {

  public abstract
  //@formatter:off
  <In extends Serializable,
   Out extends Serializable,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
 BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException;
  
  protected abstract Throwable tryCompileCycle(BuildCycleException e);
  
}
