package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildCycle.Result.UnitResultTuple;
import org.sugarj.cleardep.build.BuildCycleException.CycleState;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.dependency.DuplicateBuildUnitPathException;
import org.sugarj.cleardep.dependency.DuplicateFileGenerationException;
import org.sugarj.cleardep.dependency.FileRequirement;
import org.sugarj.cleardep.dependency.Requirement;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager implements BuildUnitProvider {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends BuildOutput> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    return build(buildReq, null);
  }

  public static <Out extends BuildOutput> Out build(BuildRequest<?, Out, ?, ?> buildReq, Map<? extends Path, Stamp> editedSourceFiles) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(editedSourceFiles);
      activeManagers.put(current, manager);
    }

    try {
      return manager.require(buildReq).getBuildResult();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  public static <Out extends BuildOutput> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs) {
    return buildAll(buildReqs, null);
  }

  public static <Out extends BuildOutput> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs, Map<? extends Path, Stamp> editedSourceFiles) {
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
            out.add(manager.require(buildReq).getBuildResult());
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

  private BuildRequest<?, ?, ?, ?> rebuildTriggeredBy = null;

  private transient ConsistencyManager consistencyManager;
  private transient Map<Path, BuildUnit<?>> generatedFiles;

  protected BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.executingStack = new ExecutingStack();
    this.consistencyManager = new ConsistencyManager();
    this.generatedFiles = new HashMap<>();
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends BuildOutput>
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
        BuildUnit<BuildOutput> metaBuilder = BuildUnit.read(depFile);

        depResult.requires(metaBuilder);
        depResult.requires(builderClass, metaBuilder.stamp());
        for (Path p : metaBuilder.getExternalFileDependencies()) {
          depResult.requires(p, metaBuilder.stamp());
        }
      }
    }
  }

  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends BuildOutput,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildUnit<Out> executeBuilder(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);
    int inputHash = DeepEquals.deepHashCode(builder.input);

    // First step: cycle detection
    BuildStackEntry<Out> entry = null;

    entry = this.executingStack.push(depResult);

    String taskDescription = builder.taskDescription();
    if (taskDescription != null)
      Log.log.beginTask(taskDescription, Log.CORE);

    try {
      depResult.setState(BuildUnit.State.IN_PROGESS);

      // call the actual builder
      try {
        //setUpMetaDependency(builder, depResult);

        Out out = builder.triggerBuild(depResult, this);
        depResult.setBuildResult(out);

        if (!depResult.isFinished())
          depResult.setState(BuildUnit.State.SUCCESS);
        // build(depResult, input);
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

      if (taskDescription != null)
        Log.log.endTask();
      // Do not think, that this is necessary, because execute is called by
      // requires
      // this.consistentUnits.add(assertConsistency(depResult));
      BuildStackEntry<?> poppedEntry = this.executingStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  protected CycleSupport searchForCycleSupport(BuildCycle cycle) {
    for (BuildRequirement<?> requirement : cycle.getCycleComponents()) {
      CycleSupport support = requirement.req.createBuilder().getCycleSupport();
      if (support != null && support.canCompileCycle(cycle)) {
        return support;
      }
    }
    return null;
  }

  private void tryCompileCycle(BuildCycleException e) throws Throwable {
    if (e.getCycleState() == CycleState.UNHANDLED) {

      Log.log.log("Detected an dependency cycle", Log.CORE);

      e.setCycleState(CycleState.NOT_RESOLVED);
      BuildCycle cycle = new BuildCycle(e.getCycleComponents());
      CycleSupport cycleSupport = this.searchForCycleSupport(cycle);
      if (cycleSupport == null) {
        throw e;
      }

      Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(cycle), Log.CORE);
      BuildCycle.Result result = cycleSupport.compileCycle(this, cycle);
      e.setCycleResult(result);
      Log.log.endTask();

      e.setCycleState(CycleState.RESOLVED);
      throw e;
    } else {

      throw e;
    }
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends BuildOutput,
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
      }
    } else {
      // Kill depending builders
      throw e;
    }
  }

  @Override
  //@formatter:off
  public
    <In extends Serializable,
     Out extends BuildOutput,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    if (rebuildTriggeredBy == null) {
      rebuildTriggeredBy = buildReq;
      Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
    }

    try {
      Builder<In, Out> builder = buildReq.createBuilder();
      Path dep = builder.persistentPath();
      BuildUnit<Out> depResult = BuildUnit.read(dep);

      this.consistencyManager.startCheckProgress(depResult);

      resetGenBy(dep, depResult);

      if (depResult == null)
        return executeBuilder(builder, dep, buildReq);

      if (!depResult.getGeneratedBy().deepEquals(buildReq))
        return executeBuilder(builder, dep, buildReq);

      if (this.consistencyManager.isConsistent(depResult))
        return depResult;

      if (!depResult.isConsistentNonrequirements())
        return executeBuilder(builder, dep, buildReq);

      for (Requirement req : depResult.getRequirements()) {
        if (req instanceof FileRequirement) {
          FileRequirement freq = (FileRequirement) req;
          if (!freq.isConsistent())
            return executeBuilder(builder, dep, buildReq);
        } else if (req instanceof BuildRequirement) {
          if (this.consistencyManager.canCheckUnit(depResult, (BuildRequirement<?>) req)) {
            BuildRequirement<?> breq = (BuildRequirement<?>) req;
            require(breq.req);
          }
        }
      }

      Set<BuildRequirement<?>> inconsistentCylicUnits = this.consistencyManager.stopCheckProgress(depResult, true);
      for (BuildRequirement<?> inconsistentUnit : inconsistentCylicUnits) {
        require(inconsistentUnit.req);
      }
      return depResult;
    } finally {
      if (rebuildTriggeredBy == buildReq)
        Log.log.endTask();
    }
  }

  private <Out extends BuildOutput> BuildUnit<Out> assertConsistency(BuildUnit<Out> depResult) {
    BuildUnit<?> other = generatedFiles.put(depResult.getPersistentPath(), depResult);
    if (other != null)
      throw new DuplicateBuildUnitPathException("Build unit " + depResult + " has same persistent path as build unit " + other);

    for (FileRequirement freq : depResult.getGeneratedFileRequirements()) {
      other = generatedFiles.put(freq.path, depResult);
      if (other != null)
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
