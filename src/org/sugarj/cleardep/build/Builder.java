package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.Log;
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
    Path dep = persistentPath(input);
    E depResult = CompilationUnit.readConsistent(resultClass(), mode, context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    
    depResult = CompilationUnit.create(resultClass(), defaultStamper(), mode, null, dep);
    String taskDescription = taskDescription(input);
    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);
      
      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);
      
      // call the actual builder
      build(depResult, input);
      
      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch(RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.addModuleDependency(required.result);
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      
      e.addBuilder(this, input, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(this, input, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
    }
    
    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(this, input, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }
  
}
