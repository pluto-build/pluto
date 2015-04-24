package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildCycleException.CycleState;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager extends BuildUnitProvider {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends Output> BuildUnit<Out> readResult(BuildRequest<?, Out, ?, ?> buildReq) throws IOException {
    return BuildUnit.read(buildReq.createBuilder().persistentPath());
  }

  public static void clean(boolean dryRun, BuildRequest<?, ?, ?, ?> req) throws IOException {
    BuildUnit<?> unit = BuildManager.readResult(req);
    if (unit == null)
      return;
    Set<BuildUnit<?>> allUnits = unit.getTransitiveModuleDependencies();
    for (BuildUnit<?> next : allUnits) {
      for (File p : next.getGeneratedFiles())
        deleteFile(p.toPath(), dryRun);
      deleteFile(next.getPersistentPath().toPath(), dryRun);
    }
  }
  private static void deleteFile(Path p, boolean dryRun) throws IOException {
    Log.log.log("Delete " + p + (dryRun ? " (dry run)" : ""), Log.CORE);
    if (!dryRun)
      if (!Files.isDirectory(p) || Files.list(p).findAny().isPresent())
        FileCommands.delete(p);
  }

  
  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager();
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

  public static <Out extends Output> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager();
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

  private ExecutingStack executingStack;
  private transient RequireStack requireStack;
  private transient boolean initialRequest = true;
  private transient DynamicAnalysis analysis;

  protected BuildManager() {
    this.executingStack = new ExecutingStack();
    // this.consistencyManager = new ConsistencyManager();
    this.analysis = new DynamicAnalysis();
    this.requireStack = new RequireStack();
  }

  // @formatter:off
  protected static 
    <In extends Serializable,
     Out extends Output>
  // @formatter:on
  void setUpMetaDependency(Builder<In, Out> builder, BuildUnit<Out> depResult) throws IOException {
    if (depResult != null) {
      File builderClass = FileCommands.getRessourcePath(builder.getClass()).toFile();
      
      File depFile = DynamicAnalysis.XATTR.getGenBy(builderClass);
      if (depFile != null && depFile.exists()) {
        BuildUnit<Output> metaBuilder = BuildUnit.read(depFile);
        depResult.requireMeta(metaBuilder);
      }
      
      depResult.requires(builderClass, LastModifiedStamper.instance.stampOf(builderClass));
    }
  }

  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildRequirement<Out> executeBuilder(Builder<In, Out> builder, File dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    this.requireStack.beginRebuild(dep);

    analysis.reset(BuildUnit.read(dep));
    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);

    setUpMetaDependency(builder, depResult);
    
    // First step: cycle detection
    this.executingStack.push(buildReq);

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
      throw RequiredBuilderFailed.init(new BuildRequirement<Out>(depResult, buildReq), e);

    } finally {
      if (!depResult.isFinished())
        depResult.setState(BuildUnit.State.FAILURE);
      depResult.write();
      if (taskDescription != null)
        Log.log.endTask(depResult.getState() == BuildUnit.State.SUCCESS);

      analysis.check(depResult, inputHash);
      assertConsistency(depResult);

      this.executingStack.pop(buildReq);
      this.requireStack.finishRebuild(dep);
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(new BuildRequirement<Out>(depResult, buildReq), new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return new BuildRequirement<Out>(depResult, buildReq);
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    // Only try to compile a cycle which is unhandled
    if (e.getCycleState() != CycleState.UNHANDLED) {
      return e;
    }

    Log.log.log("Detected a dependency cycle with root " + e.getCycleCause().createBuilder().persistentPath(), Log.CORE);

    e.setCycleState(CycleState.NOT_RESOLVED);
    BuildCycle cycle = e.getCycle();
    CycleSupport cycleSupport = cycle.findCycleSupport().orElseThrow(() -> e);
   
    Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(cycle), Log.CORE);
    try {
      BuildCycleResult result = cycleSupport.compileCycle(this, cycle);
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
     Out extends Output,
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
      Out result = e.getCycleResult().getResult(buildReq);
      if (result == null) {
        throw new AssertionError("Cyclic builder does not provide a result for " + depResult.getPersistentPath());
      }
      depResult.setBuildResult(result);
      requireStack.markConsistent(depResult.getPersistentPath());
    } else {
      depResult.setState(State.FAILURE);
    }
    Log.log.log("Stopped because of cycle", Log.CORE);
    if (e.isFirstInvokedOn(buildReq)) {
      if (e.getCycleState() != CycleState.RESOLVED) {
        Log.log.log("Unable to find builder which can compile the cycle", Log.CORE);
        // Cycle cannot be handled
        throw new RequiredBuilderFailed(new BuildRequirement<Out>(depResult, buildReq), e);
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
     Out extends Output,
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
      BuildRequirement<Out> result = require(buildReq);
      successful = !result.getUnit().hasFailed();
      return result.getUnit();
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
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    B builder = buildReq.createBuilder();
    File dep = builder.persistentPath();
    BuildUnit<Out> depResult = BuildUnit.read(dep);

    // Dont execute require because it is cyclic, requireStack keeps track of
    // this

    // Need to check that before putting dep on the requires Stack because
    // otherwise dep has always been required
    boolean alreadyRequired = requireStack.push(dep);
    boolean executed = false;
    try {
      boolean knownInconsistent = requireStack.isKnownInconsistent(dep);
      boolean noUnit = depResult == null;
      boolean changedInput = noUnit ? false : !depResult.getGeneratedBy().deepEquals(buildReq);
      boolean inconsistentNoRequirements = noUnit ? false : !depResult.isConsistentNonrequirements();
      boolean localInconsistent = knownInconsistent || noUnit || changedInput || inconsistentNoRequirements;
      Log.log.log("Locally consistent " + !localInconsistent + ":" + (knownInconsistent ? "knownInconsistent, " : "") + (noUnit ? "noUnit, " : "") + (changedInput ? "changedInput, " : "") + (inconsistentNoRequirements ? "inconsistentNoReqs, " : ""), Log.CORE);
      if (localInconsistent) {
        // Local inconsistency should execute the builder regardless whether it
        // has been required to detect the cycle
        // TODO should inconsistent file requirements trigger the same, they should i think
        executed = true;
        return executeBuilder(builder, dep, buildReq);
      }

      if (alreadyRequired) {
        return yield(buildReq, builder, depResult);
      }

      if (requireStack.isConsistent(dep))
        return yield(buildReq, builder, depResult);

      for (Requirement req : depResult.getRequirements()) {
        if (!req.isConsistentInBuild(this)) {
          executed = true;
          // Could get consistent because it was part of a cycle which is
          // compiled now
          // TODO better remove that for security purpose?
          if (requireStack.isConsistent(dep))
            return yield(buildReq, builder, depResult);
          return executeBuilder(builder, dep, buildReq);
        }
      }
      requireStack.markConsistent(dep);

    } catch (RequiredBuilderFailed e) {
      if (executed || e.getLastAddedBuilder().getUnit().getPersistentPath().equals(depResult.getPersistentPath()))
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

    return yield(buildReq, builder, depResult);
  }
  
  //@formatter:off
  private
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildRequirement<Out> yield(BuildRequest<In, Out, B, F> req, B builder, BuildUnit<Out> unit) {
    if (unit.hasFailed()) {
      RequiredBuilderFailed e = new RequiredBuilderFailed(new BuildRequirement<Out>(unit, req), "no rebuild of failing builder");
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw e;
    }
    return new BuildRequirement<>(unit, req);
  }

  private <Out extends Output> void assertConsistency(BuildUnit<Out> depResult) {
    assert depResult.isConsistentShallowReason() == InconsistenyReason.NO_REASON : "Build manager does not guarantee soundness, got consistency status " + depResult.isConsistentShallowReason() + " for " + depResult.getPersistentPath();
  }
}
