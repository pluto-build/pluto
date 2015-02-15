package org.sugarj.cleardep.build;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class BuildManager {

  
  public <C extends BuildContext, T, E extends CompilationUnit> E require(Builder<C, T, E> builder, T input, Mode<E> mode) throws IOException {
    if (builder.context.getBuildManager() != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }
    
    Path dep = builder.persistentPath(input);
    E depResult = CompilationUnit.readConsistent(builder.resultClass(), mode, builder.context.getEditedSourceFiles(), dep);
    if (depResult != null)
      return depResult;
    
    depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);
    String taskDescription = builder.taskDescription(input);
    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);
      
      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);
      
      // call the actual builder
      builder.build(depResult, input);
      
      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch(RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.addModuleDependency(required.result);
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      
      e.addBuilder(builder, input, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, input, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
    }
    
    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, input, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }
  
}
