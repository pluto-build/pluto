package org.sugarj.cleardep.build;

import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.DEPENDENCIES_ARE_REBUILT;
import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.FILES_NOT_CONSISTENT;
import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.NO_REASON;

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
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class BuildManager {

  private final Map<? extends Path, Stamp> editedSourceFiles;
  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();

  private Map<Path, InconsistenyReason> extendedInconsistencyMap = new HashMap<>();
  
  private Builder<?,?> rebuildTriggeredBy = null;

  public BuildManager() {
    this(null);
  }

  public BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
  }

  private <E extends CompilationUnit> InconsistenyReason getInconsistencyReason(Path dep) throws IOException {
    Objects.requireNonNull(dep);
    InconsistenyReason inconsistency = extendedInconsistencyMap.get(dep);
    if (inconsistency != null) {
      return inconsistency;
    }
    throw new AssertionError("Caller did not ensures that unit has been cached");
  }

  private <E extends CompilationUnit> boolean isConsistent(E depResult) throws IOException {
    if (depResult == null)
      return false;
    
    InconsistenyReason inconsistency = extendedInconsistencyMap.get(depResult.getPersistentPath());
    if (inconsistency != null) {
      return inconsistency == NO_REASON;
    }
    fillInconsistentCache(depResult);
    inconsistency = extendedInconsistencyMap.get(depResult.getPersistentPath());
    if (inconsistency == null) {
      throw new AssertionError("Cache not filled up correctly");
    }
    return inconsistency == NO_REASON;
  }

  private <E extends CompilationUnit> void fillInconsistentCache(E rootUnit) throws IOException {
    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(rootUnit));

    for (final Set<CompilationUnit> scc : sccs) {
      fillInconsistentCacheForScc(scc);
    }
  }

  private void fillInconsistentCacheForScc(final Set<CompilationUnit> scc) {
    boolean sccConsistent = true;
    for (CompilationUnit unit : scc) {
      InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
      if (reason == null) {
        reason = unit.isConsistentShallowReason(this.editedSourceFiles);
        if (reason.compareTo(DEPENDENCIES_ARE_REBUILT) < 0) {
          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep) && extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
              reason = DEPENDENCIES_ARE_REBUILT;
              break;
            }
          }
        }
      }
      sccConsistent &= reason == NO_REASON;
      extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
    }
    if (!sccConsistent && scc.size() > 1) {
      for (CompilationUnit unit : scc) {
        if (extendedInconsistencyMap.get(unit.getPersistentPath()).compareTo(DEPENDENCIES_ARE_REBUILT) < 0) {
          extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_ARE_REBUILT);
        }
      }
    }
  }

  private void updateInconsistentCacheForScc(final Set<CompilationUnit> scc) {
    boolean sccConsistent = true;
    for (CompilationUnit unit : scc) {
      InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
      if (reason != NO_REASON) {
        if (reason == null || reason.compareTo(DEPENDENCIES_ARE_REBUILT) >= 0) {
          reason = unit.isConsistentShallowReason(this.editedSourceFiles);
        } else {
          reason = NO_REASON;
        }

        if (reason.compareTo(DEPENDENCIES_ARE_REBUILT) <= 0) {
          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep) && extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
              reason = DEPENDENCIES_ARE_REBUILT;
              break;
            }
          }
        }

        sccConsistent &= reason == NO_REASON;
        extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
      }
    }
    if (!sccConsistent && scc.size() > 1) {
      for (CompilationUnit unit : scc) {
        if (extendedInconsistencyMap.get(unit.getPersistentPath()).compareTo(NO_REASON) == 0) {
          extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_ARE_REBUILT);
        }
      }
    }
  }

  private <T extends Serializable, E extends CompilationUnit> E scheduleRequire(Builder<T, E> builder, Path dep, E depResult) throws IOException {
    if (builder.manager != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }
    
    if (depResult == null)
      depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), dep, new BuildRequirement<>(builder.sourceFactory, builder.input));

    // TODO query builder for cycle

    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(depResult));
    // Find the top scc
    int topMostFileInconsistentScc;
    for (topMostFileInconsistentScc = sccs.size() - 1; topMostFileInconsistentScc >= 0; topMostFileInconsistentScc--) {
      boolean sccFileInconsistent = false;
      for (CompilationUnit unit : sccs.get(topMostFileInconsistentScc)) {
        InconsistenyReason reason = getInconsistencyReason(unit.getPersistentPath());
        if (reason.compareTo(FILES_NOT_CONSISTENT) >= 0) {
          BuildRequirement<?, ?, ?, ?> source = unit.getGeneratedBy();
          source.createBuilderAndRequire(this);
          sccFileInconsistent = true;
        }
      }
      if (sccFileInconsistent) {
        break;
      }
    }

    // Now we need to check all the units above
    for (int index = topMostFileInconsistentScc + 1; index >= 0 && index < sccs.size(); index++) {
      updateInconsistentCacheForScc(sccs.get(index));
      for (CompilationUnit unit : sccs.get(index)) {
        if (getInconsistencyReason(unit.getPersistentPath()).compareTo(NO_REASON) > 0) {
          BuildRequirement<?, ?, ?, ?> source = unit.getGeneratedBy();
          source.createBuilderAndRequire(this);
        }
      }
    }

    if (getInconsistencyReason(depResult.getPersistentPath()).compareTo(NO_REASON) > 0) {
      throw new AssertionError("BuildManager does not ensure that returned unit is consistent");
    }
    return depResult;
  }

  protected <T extends Serializable, E extends CompilationUnit> E executeBuilder(Builder<T, E> builder) throws IOException {

    Path dep = builder.persistentPath();
    E depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), dep, new BuildRequirement<>(builder.sourceFactory, builder.input));
    
    BuildStackEntry entry = new BuildStackEntry(builder.sourceFactory, dep);

    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep);
    }
    this.requireCallStack.push(entry);

    String taskDescription = builder.taskDescription();

    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);

      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);

      // call the actual builder

      builder.triggerBuild(depResult);
      // build(depResult, input);

      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch (RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.addModuleDependency(required.result, required.builder.getRequirement());
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();

      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
      extendedInconsistencyMap.put(dep, NO_REASON);
      BuildStackEntry poppedEntry = requireCallStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  public <T extends Serializable, E extends CompilationUnit> E require(Builder<T, E> builder) throws IOException {

    if (builder.manager != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }

    Path dep = builder.persistentPath();
    E depResult = CompilationUnit.read(builder.resultClass(), dep, new BuildRequirement<>(builder.sourceFactory, builder.input));

    if (depResult != null && this.isConsistent(depResult)) {
      if (!depResult.isConsistent(this.editedSourceFiles))
        throw new AssertionError("BuildManager does not guarantee soundness");
      return depResult;
    }
    
    if (depResult == null) {
      extendedInconsistencyMap.put(dep, InconsistenyReason.OTHER);
    }

    InconsistenyReason reason = getInconsistencyReason(dep);
    
    // No recursion of current unit has changed files
    if (reason.compareTo(FILES_NOT_CONSISTENT) >= 0) {
      return executeBuilder(builder);
    } else {
      // incremental rebuild
      if (rebuildTriggeredBy == null) {
        rebuildTriggeredBy = builder;
        Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
      }
      try {
        return scheduleRequire(builder, dep, depResult);
      } finally {
        if (rebuildTriggeredBy == builder)
          Log.log.endTask();
      }
    }

  }
}
