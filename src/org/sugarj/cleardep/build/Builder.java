package org.sugarj.cleardep.build;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.path.Path;

public abstract class Builder<T> {
  protected final BuildContext context;
  
  public Builder(BuildContext context) {
    this.context = context;
  }
  
  public abstract Class<T> type();
  public abstract void build(CompilationUnit result, T input);
  
  public <U> CompilationUnit require(String buildType, Path dep, Class<? extends CompilationUnit> depCl, U input, Class<U> inputType) {
    Builder<U> unit = context.findBuildUnit(buildType, inputType);
    
    CompilationUnit depResult = CompilationUnit.readConsistent(depCl, stamper, mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    depResult = CompilationUnit.create(depCl, stamper, mode, syn, dep);
    
    unit.build(depResult, input);
  }
}
