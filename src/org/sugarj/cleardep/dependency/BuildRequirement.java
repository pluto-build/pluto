package org.sugarj.cleardep.dependency;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.output.BuildOutput;

public class BuildRequirement<Out extends BuildOutput> implements Requirement {
  private static final long serialVersionUID = 6148973732378610648L;

  public final BuildUnit<Out> unit;
  public final BuildRequest<?, Out, ?, ?> req;
  
  public BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    this.unit = unit;
    this.req = req;
  }
  
  @Override
  public boolean isConsistent() {
    return unit.getGeneratedBy().deepEquals(req);
  }
  
  @Override
  public String toString() {
    return req.toString();
  }
}
