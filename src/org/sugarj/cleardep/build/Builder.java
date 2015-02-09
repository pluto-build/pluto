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
  
  protected abstract Class<E> resultClass();
  protected abstract Stamper defaultStamper();
  protected abstract void build(E result, T input);
  
  public CompilationUnit require(T input, Path dep, Mode<E> mode) throws IOException {
    E depResult = CompilationUnit.readConsistent(resultClass(), mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    
    depResult = CompilationUnit.create(resultClass(), defaultStamper(), mode, null, dep);
    build(depResult, input);
    
    return depResult;
  }
  
}
