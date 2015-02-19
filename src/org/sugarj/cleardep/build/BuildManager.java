package org.sugarj.cleardep.build;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.InconsistenyReason;
import org.sugarj.cleardep.GraphUtils;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.*;

public class BuildManager {

  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();

  private Map<Path, InconsistenyReason> extendedInconsistencyMap = new HashMap<>();

  
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
  
  private <E extends CompilationUnit> void fillInconsistentCache(Path path,Class<E> resultClass, Mode<E> mode, BuildContext context ) throws IOException{
    CompilationUnit rootUnit = CompilationUnit.read(resultClass, mode, path);

    if (rootUnit == null) {
      extendedInconsistencyMap.put(path, OTHER);
    } else {
      fillInconsistentCache(rootUnit, context);
    }
    
  }
    
  private void fillInconsistentCache(CompilationUnit root, BuildContext context) {
    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(root));

    for (Set<CompilationUnit> scc : sccs) {
      boolean sccConsistent = true;
      for (CompilationUnit unit : scc) {
        InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
        if (reason == null) {
          reason = NO_REASON;

          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep)) {
              if (extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
                reason = DEPENDENCIES_NOT_CONSISTENT;
                sccConsistent = false;
              }
            }
          }
          InconsistenyReason localReason = unit.isConsistentShallowReason(context.getEditedSourceFiles());
          if (reason == NO_REASON) {
            reason = localReason;
            sccConsistent = localReason == NO_REASON;
          } else if (localReason == FILES_NOT_CONSISTENT || localReason == OTHER) {
            reason = localReason;
          }
        }
        if (reason != NO_REASON) {
          sccConsistent = false;
        }
        extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
      }
      if (!sccConsistent && scc.size() > 1) {
        for (CompilationUnit unit : scc) {
          if (extendedInconsistencyMap.get(unit.getPersistentPath()) == NO_REASON) {
            extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_NOT_CONSISTENT);
          }
        }
      }
    }
  }

  public <C extends BuildContext, T, E extends CompilationUnit> E require(Builder<C, T, E> builder, T input, Mode<E> mode) throws IOException {

    if (builder.context.getBuildManager() != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }

    Path dep = builder.persistentPath(input);

    if (this.isConsistent(dep, builder.resultClass(), mode, builder.context)) {
      E depResult = CompilationUnit.read(builder.resultClass(), mode, dep);
      if (!depResult.isConsistent(builder.context.getEditedSourceFiles(), mode)) {
        throw new AssertionError("BuildManager does not guarantee soundness");
      }
      return depResult;
    }

    BuildStackEntry entry = new BuildStackEntry(builder, dep);

    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep);
    }
    this.requireCallStack.push(entry);

    // E depResult = CompilationUnit.readConsistent(builder.resultClass(), mode,
    // builder.context.getEditedSourceFiles(), dep);
    // if (depResult != null)
    // return depResult;

    E depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);
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
}
