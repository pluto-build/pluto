package org.sugarj.cleardep.dependency;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.build.BuildRequest;

public class BuildRequirement implements Requirement {
  private static final long serialVersionUID = 6148973732378610648L;

  public final CompilationUnit unit;
  public final BuildRequest<?, ?, ?, ?> req;
  
  public BuildRequirement(CompilationUnit unit, BuildRequest<?, ?, ?, ?> req) {
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
