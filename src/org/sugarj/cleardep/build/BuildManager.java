package org.sugarj.cleardep.build;

import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.FILES_NOT_CONSISTENT;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.InconsistenyReason;
import org.sugarj.cleardep.GraphUtils;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager {

  private final Map<? extends Path, Stamp> editedSourceFiles;
  private InconsistencyCache inconsistencyCache;
  private RequireStack requireStack;

  private Builder<?, ?> rebuildTriggeredBy = null;

  public BuildManager() {
    this(null);
  }

  public BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.inconsistencyCache = new InconsistencyCache(editedSourceFiles);
    this.requireStack = new RequireStack();
  }

  private <E extends CompilationUnit> boolean isConsistent(E depResult) throws IOException {
    if (depResult == null)
      return false;

    Path depPath = depResult.getPersistentPath();

    // If the unit is consistent in the cache stop here
    if (this.inconsistencyCache.isConsistentTry(depPath))
      return true;

    // Otherwise fill the cache for this unit
    this.inconsistencyCache.fillFor(depResult);
    // Now we know that there is a cache entry
    return this.inconsistencyCache.isConsistentSure(depPath);
  }

  private <E extends CompilationUnit> E scheduleRequire(E depResult) throws IOException {
    
    // TODO query builder for cycle

    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(depResult));
    // Find the top scc
    int topMostFileInconsistentScc;
    for (topMostFileInconsistentScc = sccs.size() - 1; topMostFileInconsistentScc >= 0; topMostFileInconsistentScc--) {
      boolean sccFileInconsistent = false;
      for (CompilationUnit unit : sccs.get(topMostFileInconsistentScc)) {
        InconsistenyReason reason = this.inconsistencyCache.getInconsistencyReasonSure(unit.getPersistentPath());
        if (reason.compareTo(FILES_NOT_CONSISTENT) >= 0) {
          BuildRequirement<?, ?, ?, ?> source = unit.getGeneratedBy();
          this.require(source);
          sccFileInconsistent = true;
        }
      }
      if (sccFileInconsistent) {
        break;
      }
    }

    // Now we need to check all the units above
    for (int index = topMostFileInconsistentScc + 1; index >= 0 && index < sccs.size(); index++) {
      this.inconsistencyCache.updateCacheForScc(sccs.get(index));
      for (CompilationUnit unit : sccs.get(index)) {
        if (!this.inconsistencyCache.isConsistentSure(unit.getPersistentPath())) {
          BuildRequirement<?, ?, ?, ?> source = unit.getGeneratedBy();
          this.require(source);
        }
      }
    }

    if (!this.inconsistencyCache.isConsistentSure(depResult.getPersistentPath())) {
      throw new AssertionError("BuildManager does not ensure that returned unit is consistent");
    }
    return depResult;
  }

  private <T extends Serializable, E extends CompilationUnit> void setUpMetaDependency(Builder<T, E> builder, E depResult) throws IOException {
    if (depResult != null) {
      // require the meta builder...
      URL res = builder.getClass().getResource(builder.getClass().getSimpleName() + ".class");
      Path builderClass = new AbsolutePath(res.getFile());
      Path depFile = Xattr.getDefault().getGenBy(builderClass);
      if (!FileCommands.exists(depFile)) {
        Log.log.logErr("Warning: Builder was not built using meta builder. Consistency for builder changes are not tracked...", Log.DETAIL);
      } else {
        CompilationUnit metaBuilder = CompilationUnit.readUnchecked(CompilationUnit.class, depFile);

        depResult.addModuleDependency(metaBuilder);
        depResult.addExternalFileDependency(builderClass);
        for (Path p: metaBuilder.getExternalFileDependencies()) {
          depResult.addExternalFileDependency(p);
        }
      }
    }
  }
  
  protected
  < T extends Serializable, 
    E extends CompilationUnit,
    B extends Builder<T, E>,
    F extends BuilderFactory<T, E, B>
    > E executeBuilder(Builder<T, E> builder, E depResult) throws IOException {

    Path dep = depResult.getPersistentPath();
    
    BuildStackEntry entry = this.requireStack.push(builder.sourceFactory, dep);

    String taskDescription = builder.taskDescription();
    int inputHash = DeepEquals.deepHashCode(builder.input);

    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);

      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);

      setUpMetaDependency(builder, depResult);
      
      // call the actual builder

      builder.triggerBuild(depResult);
      // build(depResult, input);

      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch (RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.addModuleDependency(required.result);
      depResult.setState(CompilationUnit.State.FAILURE);

      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();

      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(CompilationUnit.State.FAILURE);
      
      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();
      
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
      this.inconsistencyCache.setConsistent(dep);
      BuildStackEntry poppedEntry = this.requireStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  public <T extends Serializable, E extends CompilationUnit, B extends Builder<T, E>, F extends BuilderFactory<T, E, B>> E require(BuildRequirement<T, E, B, F> buildReq) throws IOException {

    Builder<T, E> builder = buildReq.createBuilder(this);

    Path dep = builder.persistentPath();
    E depResult = CompilationUnit.read(builder.resultClass(), dep, buildReq);
    
    if (depResult == null) {
      this.inconsistencyCache.set(dep, InconsistenyReason.OTHER);
    } else if (this.isConsistent(depResult)) {
      if (!depResult.isConsistent(this.editedSourceFiles))
        throw new AssertionError("BuildManager does not guarantee soundness");
      
      return depResult;
    }

    // We know that there is a cache entry because it has been either manually
    // set or filledUp caused by isConsistent call.
    InconsistenyReason reason = this.inconsistencyCache.getInconsistencyReasonSure(dep);

    // No recursion of current unit has changed files
    if (reason.compareTo(FILES_NOT_CONSISTENT) >= 0) {
      depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), dep, buildReq);
      return executeBuilder(builder, depResult);
    } else {
      // incremental rebuild
      if (rebuildTriggeredBy == null) {
        rebuildTriggeredBy = builder;
        Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
      }
      try {
        if (depResult == null)
          depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), dep, buildReq);

        return scheduleRequire(depResult);
      } finally {
        if (rebuildTriggeredBy == builder)
          Log.log.endTask();
      }
    }

  }
}
