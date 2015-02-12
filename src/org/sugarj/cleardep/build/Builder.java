package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public abstract class Builder<C extends BuildContext, T, E extends CompilationUnit> {
  protected final C context;
  
  public Builder(C context) {
    this.context = context;
  }
  
  protected abstract Path persistentPath(T input);
  protected abstract Class<E> resultClass();
  protected abstract Stamper defaultStamper();
  protected abstract void build(E result, T input) throws IOException;
  
  public CompilationUnit require(T input, Mode<E> mode) throws IOException {
    Path dep = persistentPath(input);
    E depResult = CompilationUnit.readConsistent(resultClass(), mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    
    depResult = CompilationUnit.create(resultClass(), defaultStamper(), mode, null, dep);
    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);
      build(depResult, input);
      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch (Throwable t) {
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      t.printStackTrace(Log.err);
      return depResult;
    }
    
    return depResult;
  }
  
}
