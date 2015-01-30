package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.common.path.Path;

public abstract class Builder<T> {
  protected final BuildContext context;
  
  public Builder(BuildContext context) {
    this.context = context;
  }
  
  public abstract CompilationUnit init(Path dep);
  public abstract void build(CompilationUnit result, T input);
  
  
  public <U> CompilationUnit require(Builder<U> builder, U input, Path dep, Mode<CompilationUnit> mode) throws IOException {
    CompilationUnit depResult = CompilationUnit.readConsistent(mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    depResult = builder.init(dep);
    
    builder.build(depResult, input);
    return depResult;
  }
}
