package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

public abstract class Builder<T, E extends CompilationUnit> {
  protected final BuildContext context;
  
  public Builder(BuildContext context) {
    this.context = context;
  }
  
  public abstract Class<E> resultClass();
  public abstract Stamper defaultStamper();
  public abstract void build(CompilationUnit result, T input);
  
  
  public <U, F extends CompilationUnit> CompilationUnit require(Builder<U, F> builder, U input, Path dep, Mode<F> mode) throws IOException {
    CompilationUnit depResult = CompilationUnit.readConsistent(builder.resultClass(), mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    
    depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);
    builder.build(depResult, input);
    
    return depResult;
  }
  
//  public <U, F extends CompilationUnit> CompilationUnit synthesize(Builder<U, F> builder, U input, Path dep, Mode<F> mode) throws IOException {
//    CompilationUnit depResult = CompilationUnit.readConsistent(builder.resultClass(), mode, context.getEditedSourceFiles(), dep);
//    if (depResult != null)
//      return depResult;
//    
//    depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);
//    builder.build(depResult, input);
//    
//    return depResult;
//  }
//  
  
}
