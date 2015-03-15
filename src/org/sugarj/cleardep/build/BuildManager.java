package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildCycle.Result.UnitResultTuple;
import org.sugarj.cleardep.build.BuildCycleException.CycleState;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.dependency.BuildOutputRequirement;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.dependency.DuplicateBuildUnitPathException;
import org.sugarj.cleardep.dependency.DuplicateFileGenerationException;
import org.sugarj.cleardep.dependency.FileRequirement;
import org.sugarj.cleardep.dependency.Requirement;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager extends BuildUnitProvider {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends Serializable> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    return build(buildReq, null);
  }

  public static <Out extends Serializable> Out build(BuildRequest<?, Out, ?, ?> buildReq, Map<? extends Path, Stamp> editedSourceFiles) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(editedSourceFiles);
      activeManagers.put(current, manager);
    }

    try {
      return manager.requireInitially(buildReq).getBuildResult();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  public static <Out extends Serializable> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs) {
    return buildAll(buildReqs, null);
  }

  public static <Out extends Serializable> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs, Map<? extends Path, Stamp> editedSourceFiles) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(editedSourceFiles);
      activeManagers.put(current, manager);
    }

    try {
      List<Out> out = new ArrayList<>();
      for (BuildRequest<?, Out, ?, ?> buildReq : buildReqs)
        if (buildReq != null)
          try {
            out.add(manager.requireInitially(buildReq).getBuildResult());
          } catch (IOException e) {
            e.printStackTrace();
            out.add(null);
          }
      return out;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  private final Map<? extends Path, Stamp> editedSourceFiles;
  private ExecutingStack executingStack;

  private transient RequireStack requireStack;

  private transient Map<Path, BuildUnit<?>> generatedFiles;

  protected BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.executingStack = new ExecutingStack();
    // this.consistencyManager = new ConsistencyManager();
    this.generatedFiles = new HashMap<Path, BuildUnit<?>>();
    this.requireStack = new RequireStack();
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends Serializable>
  // @formatter:on
  void setUpMetaDependency(Builder<In, Out> builder, BuildUnit<Out> depResult) throws IOException {
    if (depResult != null) {
      // require the meta builder...
      URL res = builder.getClass().getResource(builder.getClass().getSimpleName() + ".class");
      Path builderClass = new AbsolutePath(res.getFile());
      Path depFile = Xattr.getDefault().getGenBy(builderClass);
      if (!FileCommands.exists(depFile)) {
        Log.log.logErr("Warning: Builder was not built using meta builder. Consistency for builder changes are not tracked...", Log.DETAIL);
      } else {
        BuildUnit<Serializable> metaBuilder = BuildUnit.read(depFile);

        depResult.requires(metaBuilder);
        depResult.requires(builderClass, LastModifiedStamper.instance.stampOf(builderClass));

        // TODO: needed?
        // for (Path p : metaBuilder.getExternalFileDependencies()) {
        // depResult.requires(p, LastModifiedStamper.instance.stampOf(p));
        // }
      }
    }
  }

  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildUnit<Out> executeBuilder(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    requireStack.beginRebuild(dep);

    resetGenBy(dep, BuildUnit.read(dep));
    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);
    int inputHash = DeepEquals.deepHashCode(builder.input);

    String taskDescription = builder.taskDescription();

    BuildStackEntry<Out> entry = null;
    // First step: cycle detection

    if (taskDescription != null)
      Log.log.beginTask(taskDescription, Log.CORE);

    entry = this.executingStack.push(depResult);

    depResult.setState(BuildUnit.State.IN_PROGESS);

    try {
      try {

        // setUpMetaDependency(builder, depResult);

        // call the actual builder
        Out out = builder.triggerBuild(depResult, this);
        depResult.setBuildResult(out);

        if (!depResult.isFinished())
          depResult.setState(BuildUnit.State.SUCCESS);
        
      } catch (BuildCycleException e) {
        tryCompileCycle(e);
      }
      
    } catch (BuildCycleException e) {
      stopBuilderInCycle(builder, dep, buildReq, depResult, e);
    } catch (RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.requires(required.result);
      depResult.setState(BuildUnit.State.FAILURE);

      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(BuildUnit.State.FAILURE);

      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {

      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();
      assertConsistency(depResult);
      requireStack.finishRebuild(dep);

      if (taskDescription != null)
        Log.log.endTask();
      
      BuildStackEntry<?> poppedEntry = this.executingStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }


  @Override
  protected void tryCompileCycle(BuildCycleException e) throws Throwable {
    if (e.getCycleState() == CycleState.UNHANDLED) {

      Log.log.log("Detected a dependency cycle with root " + e.getCycleComponents().get(0).unit.getPersistentPath(), Log.CORE);

      e.setCycleState(CycleState.NOT_RESOLVED);
      BuildCycle cycle = new BuildCycle(e.getCycleComponents());
      CycleSupport cycleSupport = cycle.getCycleSupport();
      if (cycleSupport == null) {
        throw e;
      }

      Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(cycle), Log.CORE);
      try {
        try {
          BuildCycle.Result result = cycleSupport.compileCycle(this, cycle);
          e.setCycleResult(result);
          e.setCycleState(CycleState.RESOLVED);
        } catch (BuildCycleException cyclicEx) {
          e.setCycleState(cyclicEx.getCycleState());
          e.setCycleResult(cyclicEx.getCycleResult());
        } catch (Throwable t) {
          throw t;
        }
      } finally {
        Log.log.endTask();
        throw e;
      }
    } else {

      throw e;
    }
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>> 
  //@formatter:on
  void stopBuilderInCycle(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq, BuildUnit<Out> depResult, BuildCycleException e) {
    // This is the exception which has been rethrown above, but we cannot
    // handle it
    // here because compiling the cycle needs to be in the major try block
    // where normal
    // units are compiled too

    // Set the result to the unit
    if (e.getCycleState() == CycleState.RESOLVED) {
      if (e.getCycleResult() == null) {
        Log.log.log("Error: Cyclic builder does not provide a cycleResult " + e.hashCode(), Log.CORE);
        throw new AssertionError("Cyclic builder does not provide a cycleResult");
      }
      UnitResultTuple<Out> tuple = e.getCycleResult().getUnitResult(depResult);
      if (tuple == null) {
        throw new AssertionError("Cyclic builder does not provide a result for " + depResult.getPersistentPath());
      }
      tuple.setOutputToUnit();
    }
    Log.log.log("Stopped because of cycle", Log.CORE);
    if (e.isUnitFirstInvokedOn(dep, buildReq.factory)) {
      if (e.getCycleState() != CycleState.RESOLVED) {
        Log.log.log("Unable to find builder which can compile the cycle", Log.CORE);
        // Cycle cannot be handled
        throw new RequiredBuilderFailed(builder, depResult, e);
      } else {

        if (this.executingStack.getNumContains(e.getCycleComponents().get(0).unit) == 1) {
          Log.log.log("but cycle has been compiled", Log.CORE);
 
        } else {
          throw e;
        }
      }
    } else {
      // Kill depending builders
      throw e;
    }
  }

//@formatter:off
  protected
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> requireInitially(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
    try {
      return require(null, buildReq);
    } finally {
      Log.log.endTask();
    }
  }

  @Override
  //@formatter:off
  public
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> require(BuildUnit<?> source, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    Builder<In, Out> builder = buildReq.createBuilder();
    Path dep = builder.persistentPath();
    BuildUnit<Out> depResult = BuildUnit.read(dep);

    boolean localInconsistent =
        (requireStack.isKnownInconsistent(dep)) ||
        (depResult == null) ||
        (!depResult.getGeneratedBy().deepEquals(buildReq)) ||
        (!depResult.isConsistentNonrequirements());

    if (localInconsistent) {
      return executeBuilder(builder, dep, buildReq);
    }

    if (requireStack.isConsistent(dep))
      return depResult;

    // Dont execute require because it is cyclic, requireStack keeps track of
    // this
    if (source != null && requireStack.isAlreadyRequired(source.getPersistentPath(), dep)) {
      return depResult;
    }

    requireStack.beginRequire(dep);
    try {

      for (Requirement req : depResult.getRequirements()) {
        if (!req.isConsistentInBuild(depResult, this)) {
          return executeBuilder(builder, dep, buildReq);
        } else {
          // Could get consistent because it was part of a cycle which is
          // compiled now
          // TODO better remove that for security purpose?
          if (requireStack.isConsistent(dep))
            return depResult;
        }
      }
      requireStack.markConsistent(dep);

    } finally {
      if (source != null)
        requireStack.finishRequire(source.getPersistentPath(), dep);
      else
        requireStack.finishRequire(null, dep);
    }

    return depResult;

  }

  private <Out extends Serializable> BuildUnit<Out> assertConsistency(BuildUnit<Out> depResult) {
    BuildUnit<?> other = generatedFiles.put(depResult.getPersistentPath(), depResult);
    if (other != null && other != depResult)
      throw new DuplicateBuildUnitPathException("Build unit " + depResult + " has same persistent path as build unit " + other);

    for (FileRequirement freq : depResult.getGeneratedFileRequirements()) {
      other = generatedFiles.put(freq.path, depResult);
      if (other != null && other != depResult)
        throw new DuplicateFileGenerationException("Build unit " + depResult + " generates same file as build unit " + other);
    }

    // if (!depResult.isConsistent(null))
    // throw new AssertionError("Build manager does not guarantee soundness");
    return depResult;
  }

  private void resetGenBy(Path dep, BuildUnit<?> depResult) throws IOException {
    if (depResult != null)
      for (Path p : depResult.getGeneratedFiles())
        BuildUnit.xattr.removeGenBy(p);
  }
}
