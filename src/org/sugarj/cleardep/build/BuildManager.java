package org.sugarj.cleardep.build;

import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.InconsistenyReason;
import org.sugarj.cleardep.GraphUtils;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class BuildManager {

  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();

  private Map<Path, InconsistenyReason> extendedInconsistencyMap = new HashMap<>();

  private <E extends CompilationUnit> InconsistenyReason getInconsistencyReason(CompilationUnit unit) throws IOException {
    Objects.requireNonNull(unit);
    InconsistenyReason inconsistency = extendedInconsistencyMap.get(unit.getPersistentPath());
    if (inconsistency != null) {
      return inconsistency;
    }
    throw new AssertionError("Caller did not ensures that unit has been cached");
  }
  
  private <E extends CompilationUnit> boolean isConsistent(Path depPath, Class<E> resultClass, Mode<E> mode, BuildContext context) throws IOException {
    InconsistenyReason inconsistency = extendedInconsistencyMap.get(depPath);
    if (inconsistency != null) {
      return inconsistency == NO_REASON;
    }
    fillInconsistentCache(depPath, resultClass, mode, context);
    inconsistency = extendedInconsistencyMap.get(depPath);
    if (inconsistency == null) {
      throw new AssertionError("Cache not filled up correctly");
    }
    return inconsistency == NO_REASON;
  }

  private <E extends CompilationUnit> void fillInconsistentCache(Path path, Class<E> resultClass, Mode<E> mode, BuildContext context) throws IOException {
    CompilationUnit rootUnit = CompilationUnit.read(resultClass, mode, path);

    if (rootUnit == null) {
      extendedInconsistencyMap.put(path, OTHER);
    } else {
      fillInconsistentCache(rootUnit, context);
    }

  }

  private void fillInconsistentCache(CompilationUnit root, BuildContext context) {
    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(root));

    for (final Set<CompilationUnit> scc : sccs) {
      updateInconsistentCacheForScc(context, scc);
    }
  }

  private void updateInconsistentCacheForScc(BuildContext context, final Set<CompilationUnit> scc) {
    boolean sccConsistent = true;
    for (CompilationUnit unit : scc) {
      InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
      if (reason == null) {
        reason = unit.isConsistentShallowReason(context.getEditedSourceFiles());
        if (reason.compareTo(DEPENDENCIES_NOT_CONSISTENT) < 0) {
          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep) && extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
              reason = DEPENDENCIES_NOT_CONSISTENT;
              break;
            }
          }
        }
      }
      sccConsistent &= reason == NO_REASON;
      System.out.println(reason +" " + unit.getPersistentPath());
      extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
    }
    if (!sccConsistent && scc.size() > 1) {
      for (CompilationUnit unit : scc) {
        if (extendedInconsistencyMap.get(unit.getPersistentPath()).compareTo(DEPENDENCIES_NOT_CONSISTENT) < 0) {
          extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_NOT_CONSISTENT);
        }
      }
    }
  }
  
  
  
  private <C extends BuildContext, T extends Serializable, E extends CompilationUnit> E scheduleRequire(Builder<C, T, E> builder, T input, Mode<E> mode) throws IOException{
    if (builder.context.getBuildManager() != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }

    Path dep = builder.persistentPath(input);
    System.out.println("\nSchedule for: " + dep);

    E depResult;
    // = CompilationUnit.readConsistent(builder.resultClass(), mode,
    // builder.context.getEditedSourceFiles(), dep);
    // if (depResult != null)
    // return depResult;

    depResult = CompilationUnit.read(builder.resultClass(), mode, dep);

    if (this.isConsistent(dep, builder.resultClass(), mode, builder.context)) {
      if (!depResult.isConsistent(builder.context.getEditedSourceFiles(), mode)) {
        throw new AssertionError("BuildManager does not guarantee soundness");
      }
      return depResult;
    }
    
   
    
    // TODO query builder for cycle
    
    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(depResult));
    // Find the top scc
    int topMostFileInconsistentScc;
    for(topMostFileInconsistentScc  = sccs.size()-1; topMostFileInconsistentScc >= 0; topMostFileInconsistentScc--) {
      boolean sccFileInconsistent = false;
      for (CompilationUnit unit : sccs.get(topMostFileInconsistentScc)) {
        if (getInconsistencyReason(unit).compareTo(FILES_NOT_CONSISTENT) >= 0) {
          FactoryInputTuple<C, ?, ?, ?, ?> source = (FactoryInputTuple<C, ?, ?, ?, ?>) unit.getGeneratedBy();
          source.createBuilderAndRequire(builder.context);
        }
      }
      if (sccFileInconsistent) {
        break;
      }
    }
    // Now we need to check all the units above
    for (int index = topMostFileInconsistentScc +1; index <= sccs.size(); index ++) {
      updateInconsistentCacheForScc(builder.context, sccs.get(index));
      for (CompilationUnit unit : sccs.get(index)) {
        if (getInconsistencyReason(unit).compareTo(NO_REASON) > 0) {
          FactoryInputTuple<C, ?, ?, ?, ?> source = (FactoryInputTuple<C, ?, ?, ?, ?>) unit.getGeneratedBy();
          source.createBuilderAndRequire(builder.context);
        }
      }
    }
    
    if (getInconsistencyReason(depResult).compareTo(NO_REASON) > 0) {
      throw new AssertionError("BuildManager does not ensure that returned unit is consistent");
    }
    return depResult;
  }
  
  protected <C extends BuildContext, T extends Serializable, E extends CompilationUnit> E executeBuilder(Builder<C, T, E> builder, T input, Mode<E> mode) throws IOException {
    
    Path dep = builder.persistentPath(input);

    System.out.println("\nExecute for: "+ dep);
 
    
    BuildStackEntry entry = new BuildStackEntry(builder, dep);

    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep);
    }
    this.requireCallStack.push(entry);
    
    E depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);

    // E depResult = CompilationUnit.readConsistent(builder.resultClass(), mode,
    // builder.context.getEditedSourceFiles(), dep);
    // if (depResult != null)
    // return depResult;

   
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
    } catch (RequiredBuilderFailed e) {
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
      BuildStackEntry poppedEntry = requireCallStack.pop();
      if (poppedEntry != entry) {
        throw new AssertionError("Got the wrong build stack entry from the requires stack");
      }
    }

    extendedInconsistencyMap.put(dep, NO_REASON);

    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, input, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }
  

  public <C extends BuildContext, T extends Serializable, E extends CompilationUnit> E require(Builder<C, T, E> builder, T input, Mode<E> mode) throws IOException {

    if (builder.context.getBuildManager() != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }

    Path dep = builder.persistentPath(input);
    System.out.println("\n Require for " +dep);
    E depResult;
    depResult = CompilationUnit.read(builder.resultClass(), mode, dep);
    // = CompilationUnit.readConsistent(builder.resultClass(), mode,
    // builder.context.getEditedSourceFiles(), dep);
    // if (depResult != null)
    // return depResult;
    
    boolean consistent = this.isConsistent(dep, builder.resultClass(), mode, builder.context);
    
    System.out.println("Consistent "+consistent +" "+ dep);

    if (consistent) {
     
      if (!depResult.isConsistent(builder.context.getEditedSourceFiles(), mode)) {
        throw new AssertionError("BuildManager does not guarantee soundness");
      }
      return depResult;
    }
    if (depResult == null) {
      depResult   = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);

    }
    
    // No recursion of current unit has changed files
    if (getInconsistencyReason(depResult).compareTo(FILES_NOT_CONSISTENT) >= 0) {
      return executeBuilder(builder, input, mode);
    } else {
      return scheduleRequire(builder, input, mode);
    }

    
  }
}
