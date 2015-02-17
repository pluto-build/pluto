package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

public abstract class Builder<C extends BuildContext, T, E extends CompilationUnit> {
  protected final C context;
  
  public Builder(C context) {
    this.context = context;
  }
  
  /**
   * Provides the task description for the builder and its input.
   * The description is used for console logging when the builder is run.
   * 
   * @return the task description or `null` if no logging is wanted.
   */
  protected abstract String taskDescription(T input);
  protected abstract Path persistentPath(T input);
  protected abstract Class<E> resultClass();
  protected abstract Stamper defaultStamper();
  protected abstract void build(E result, T input) throws IOException;
  
  public CompilationUnit require(T input, Mode<E> mode) throws IOException {
    return this.context.getBuildManager().require(this, input, mode);
  }
  
}
