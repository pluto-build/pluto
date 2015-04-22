package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildCycle.Result.UnitResultTuple;
import build.pluto.builder.BuildCycleException.CycleState;
import build.pluto.dependency.DuplicateBuildUnitPathException;
import build.pluto.dependency.DuplicateFileGenerationException;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;
import build.pluto.xattr.Xattr;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager extends BuildUnitProvider {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends Serializable> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    return build(buildReq, null);
  }

  public static <Out extends Serializable> BuildUnit<Out> readResult(BuildRequest<?, Out, ?, ?> buildReq) throws IOException {
    return BuildUnit.read(buildReq.createBuilder().persistentPath().toFile());
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

  private transient Map<File, BuildUnit<?>> generatedFiles;

  private transient boolean initialRequest = true;

  protected BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.executingStack = new ExecutingStack();
    // this.consistencyManager = new ConsistencyManager();
    this.generatedFiles = new HashMap<>();
    this.requireStack = new RequireStack();
  }

  // @formatter:off
  protected static 
    <In extends Serializable,
     Out extends Serializable>
  // @formatter:on
  void setUpMetaDependency(Builder<In, Out> builder, BuildUnit<Out> depResult) throws IOException {
    if (depResult != null) {
      Path builderClass = FileCommands.getRessourcePath(builder.getClass());
      depResult.requires(builderClass.toFile(), LastModifiedStamper.instance.stampOf(builderClass.toFile()));
      
      Path depFile = Xattr.getDefault().getGenBy(builderClass);
      if (FileCommands.exists(depFile)) {
        BuildUnit<Serializable> metaBuilder = BuildUnit.read(depFile.toFile());
        depResult.requireMeta(metaBuilder);
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
  BuildUnit<Out> executeBuilder(Builder<In, Out> builder, File dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    this.requireStack.beginRebuild(dep);

    resetGenBy(BuildUnit.read(dep));
    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);

    setUpMetaDependency(builder, depResult);
    
    // First step: cycle detection
    this.executingStack.push(depResult);

    int inputHash = DeepEquals.deepHashCode(builder.input);

    String taskDescription = builder.description();
    if (taskDescription != null)
      Log.log.beginTask(taskDescription, Log.CORE);

    depResult.setState(BuildUnit.State.IN_PROGESS);
    
    try {
      try {
        // call the actual builder
        Out out = builder.triggerBuild(depResult, this);
        depResult.setBuildResult(out);
        if (!depResult.isFinished())
          depResult.setState(BuildUnit.State.SUCCESS);
      } catch (BuildCycleException e) {
        throw this.tryCompileCycle(e);
      }
    } catch (BuildCycleException e) {
      stopBuilderInCycle(builder, buildReq, depResult, e);

    } catch (RequiredBuilderFailed e) {
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e.enqueueBuilder(depResult, builder);

    } catch (Throwable e) {
      Log.log.logErr(e.getClass() + ": " + e.getMessage(), Log.CORE);
      throw RequiredBuilderFailed.init(builder, depResult, e);

    } finally {
      if (!depResult.isFinished())
        depResult.setState(BuildUnit.State.FAILURE);
      depResult.write();
      if (taskDescription != null)
        Log.log.endTask(depResult.getState() == BuildUnit.State.SUCCESS);

      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      assertConsistency(depResult);

      this.executingStack.pop(depResult);
      this.requireStack.finishRebuild(dep);
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    // Only try to compile a cycle which is unhandled
    if (e.getCycleState() != CycleState.UNHANDLED) {
      return e;
    }

    Log.log.log("Detected a dependency cycle with root " + e.getCycleCause().getPersistentPath(), Log.CORE);

    e.setCycleState(CycleState.NOT_RESOLVED);
    BuildCycle cycle = new BuildCycle(e.getCycleComponents());
    CycleSupport cycleSupport = cycle.getCycleSupport();
    if (cycleSupport == null) {
      return e;
    }

    Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(cycle), Log.CORE);
    try {
      BuildCycle.Result result = cycleSupport.compileCycle(this, cycle);
      e.setCycleResult(result);
      e.setCycleState(CycleState.RESOLVED);
    } catch (BuildCycleException cyclicEx) {
      // Now cycle in cycle detected, use result from it
      // But keep throw away the new exception but use
      // the existing ones to kill all builders of this
      // cycle
      e.setCycleState(cyclicEx.getCycleState());
      e.setCycleResult(cyclicEx.getCycleResult());
    } catch (Throwable t) {
      Log.log.endTask("Cyclic compilation failed: " + t.getMessage());
      return t;
    }
    Log.log.endTask();
    return e;
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>> 
  //@formatter:on
  void stopBuilderInCycle(Builder<In, Out> builder, BuildRequest<In, Out, B, F> buildReq, BuildUnit<Out> depResult, BuildCycleException e) {
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
      requireStack.markConsistent(depResult.getPersistentPath());
    } else {
      depResult.setState(State.FAILURE);
    }
    Log.log.log("Stopped because of cycle", Log.CORE);
    if (e.isUnitFirstInvokedOn(depResult)) {
      if (e.getCycleState() != CycleState.RESOLVED) {
        Log.log.log("Unable to find builder which can compile the cycle", Log.CORE);
        // Cycle cannot be handled
        throw new RequiredBuilderFailed(builder, depResult, e);
      } else {

        if (this.executingStack.getNumContains(e.getCycleCause()) == 1) {
          Log.log.log("but cycle has been compiled", Log.CORE);

        } else {
          Log.log.log("too much entries left", Log.CORE);
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
    boolean wasInitial = false;
    if (initialRequest) {
      Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
      initialRequest = false;
      wasInitial = true;
    }
    boolean successful = false;
    try {
      BuildUnit<Out> result = require(buildReq);
      successful = !result.hasFailed();
      return result;
    } finally {
      if (wasInitial) {
        Log.log.endTask(successful);
        initialRequest = true;
      }
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
  BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    Builder<In, Out> builder = buildReq.createBuilder();
    File dep = builder.persistentPath().toFile();
    BuildUnit<Out> depResult = BuildUnit.read(dep);

    // Dont execute require because it is cyclic, requireStack keeps track of
    // this

    // Need to check that before putting dep on the requires Stack because
    // otherwise dep has always been required
    boolean alreadyRequired = requireStack.push(dep);
    boolean executed = false;

    try {
      boolean localInconsistent = (requireStack.isKnownInconsistent(dep)) || (depResult == null) || (!depResult.getGeneratedBy().deepEquals(buildReq)) || (!depResult.isConsistentNonrequirements());

      if (localInconsistent) {
        // Local inconsistency should execute the builder regardless whether it
        // has been required to detect the cycle
        // TODO should inconsistent file requirements trigger the same, they should i think
        executed = true;
        return executeBuilder(builder, dep, buildReq);
      }

      if (alreadyRequired) {
        return yield(builder, depResult);
      }

      if (requireStack.isConsistent(dep))
        return yield(builder, depResult);

      for (Requirement req : depResult.getRequirements()) {
        if (!req.isConsistentInBuild(this)) {
          executed = true;
          // Could get consistent because it was part of a cycle which is
          // compiled now
          // TODO better remove that for security purpose?
          if (requireStack.isConsistent(dep))
            return yield(builder, depResult);
          return executeBuilder(builder, dep, buildReq);
        }
      }
      requireStack.markConsistent(dep);

    } catch (RequiredBuilderFailed e) {
      if (executed || e.getLastAddedBuilder().result.getPersistentPath().equals(depResult.getPersistentPath()))
        throw e;

      String desc = builder.description();
      if (desc != null)
        Log.log.log("Failing builder was required by \"" + desc + "\".", Log.CORE);
      throw e.enqueueBuilder(depResult, builder, false);
    } finally {
     // if (!alreadyRequired)
        requireStack.pop(dep);
//      
//      if (!executed && depResult.hasFailed()) {
//        Log.log.log("Required builder \"" + builder.description() + "\" failed.", Log.CORE);
//        throw new RequiredBuilderFailed(builder, depResult, "no rebuild of failing builder");
//      }
    }

    return yield(builder, depResult);
  }
  
  private <In extends Serializable, Out extends Serializable> BuildUnit<Out> yield(Builder<In, Out> builder, BuildUnit<Out> unit) {
    if (unit.hasFailed()) {
      RequiredBuilderFailed e = new RequiredBuilderFailed(builder, unit, "no rebuild of failing builder");
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw e;
    }
    return unit;
  }

  private <Out extends Serializable> BuildUnit<Out> assertConsistency(BuildUnit<Out> depResult) {
    BuildUnit<?> other = generatedFiles.put(depResult.getPersistentPath(), depResult);
    if (other != null && other != depResult)
      throw new DuplicateBuildUnitPathException("Build unit " + depResult + " has same persistent path as build unit " + other);

    for (FileRequirement freq : depResult.getGeneratedFileRequirements()) {
      other = generatedFiles.put(freq.file, depResult);
      if (other != null && other != depResult)
        throw new DuplicateFileGenerationException("Build unit " + depResult + " generates same file as build unit " + other);
    }

    InconsistenyReason reason = depResult.isConsistentShallowReason(null);
    if (reason != InconsistenyReason.NO_REASON)
      throw new AssertionError("Build manager does not guarantee soundness, got consistency status " + reason + " for " + depResult.getPersistentPath());
    return depResult;
  }

  private void resetGenBy(BuildUnit<?> depResult) throws IOException {
    if (depResult != null)
      for (File f : depResult.getGeneratedFiles())
        BuildUnit.xattr.removeGenBy(f.toPath());
  }
}
