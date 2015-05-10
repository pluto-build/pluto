package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

  private ExecutingStack executingStack;
  private transient RequireStack requireStack;
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

    this.requireStack.beginRebuild(buildReq);

    analysis.reset(BuildUnit.read(dep));
    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);

    setUpMetaDependency(builder, depResult);

    // First step: cycle detection
    this.executingStack.push(buildReq);

    int inputHash = DeepEquals.deepHashCode(builder.getInput());

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
      stopBuilderInCycle(builder, buildReq, depResult, inputHash, e);

    } catch (RequiredBuilderFailed e) {
      if (taskDescription != null)
        Log.log.logErr("Required builder failed " + e.getMessage(), Log.CORE);
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
      this.requireStack.finishRebuild(buildReq);
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

    Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(), Log.CORE);
    try {
      Set<BuildUnit<?>> resultUnits = cycleSupport.compileCycle(this);
      for (BuildUnit<?> resultUnit : resultUnits) {
        resultUnit.write();
        this.requireStack.markConsistent(resultUnit.getGeneratedBy());
      }
      e.setCycleState(CycleState.RESOLVED);
    } catch (BuildCycleException cyclicEx) {
      // Now cycle in cycle detected, use result from it
      // But keep throw away the new exception but use
      // the existing ones to kill all builders of this
      // cycle
      e.setCycleState(cyclicEx.getCycleState());
    } catch (Throwable t) {
      e.setCycleState(CycleState.RESOLVED);
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
  void stopBuilderInCycle(Builder<In, Out> builder, BuildRequest<In, Out, B, F> buildReq, BuildUnit<Out> depResult, int inputHash, BuildCycleException e) throws IOException {
    // This is the exception which has been rethrown above, but we cannot
    // handle it
    // here because compiling the cycle needs to be in the major try block
    // where normal
    // units are compiled too

    // Set the result to the unit
    if (e.getCycleState() == CycleState.RESOLVED) {
      if (depResult.getBuildResult() == null) {
        throw new AssertionError("Cyclic builder does not provide a result for " + depResult.getPersistentPath());
      }
      if (!depResult.isFinished())
        depResult.setState(State.finished(true));

      analysis.check(depResult, inputHash);
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
    Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
    boolean successful = false;
    try {
      BuildRequirement<Out> result = require(buildReq);
      successful = !result.getUnit().hasFailed();
      return result.getUnit();
    } finally {
      Log.log.endTask(successful);
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
  BuildRequirement<Out> require(final BuildRequest<In, Out, B, F> buildReq) throws IOException {

    B builder = buildReq.createBuilder();
    File dep = builder.persistentPath();
    BuildUnit<Out> depResult = BuildUnit.read(dep);

    // Dont execute require because it is cyclic, requireStack keeps track of
    // this

    // Need to check that before putting dep on the requires Stack because
    // otherwise dep has always been required
    boolean alreadyRequired = requireStack.push(buildReq);
    boolean executed = false;
    try {
      boolean knownInconsistent = requireStack.isKnownInconsistent(dep);
      boolean noUnit = depResult == null;
      boolean changedInput = noUnit ? false : !depResult.getGeneratedBy().deepEquals(buildReq);
      InconsistenyReason localInconsistencyReason = noUnit ? null : depResult.isConsistentNonrequirementsReason();
      boolean inconsistentNoRequirements = noUnit ? false : localInconsistencyReason != InconsistenyReason.NO_REASON;
      boolean localInconsistent = knownInconsistent || noUnit || changedInput || inconsistentNoRequirements;
      Log.log.log("Locally consistent " + !localInconsistent + ":" + (knownInconsistent ? "knownInconsistent, " : "") + (noUnit ? "noUnit, " : "") + (changedInput ? "changedInput, " : "") + (inconsistentNoRequirements ? "inconsistentNoReqs (" + localInconsistencyReason + "), " : ""), Log.CORE);
      if (localInconsistent) {
        // Local inconsistency should execute the builder regardless whether it
        // has been required to detect the cycle
        // TODO should inconsistent file requirements trigger the same, they
        // should i think
        executed = true;
        Log.log.log("Rebuild because locally inconsistent", Log.DETAIL);
        return executeBuilder(builder, dep, buildReq);
      }

      boolean assumptionIncomplete = requireStack.existsInconsistentCyclicRequest(buildReq);
      Log.log.log("Assumptions inconsistent " + assumptionIncomplete, Log.DETAIL);
      if (alreadyRequired) {
        if (!assumptionIncomplete) {
          return yield(buildReq, builder, depResult);
        } else {
          Log.log.log("Deptected Require cycle for " + dep, Log.DETAIL);
          BuildCycle cycle = requireStack.createCycleFor(buildReq);
          cycle = new BuildCycle(executingStack.topMostEntry(cycle.getCycleComponents()), cycle.getCycleComponents());
          throw new BuildCycleException("Require build cycle on " + dep, cycle.getInitial(), cycle);
        }
      }

      if (requireStack.isConsistent(buildReq))
        return yield(buildReq, builder, depResult);

      for (Requirement req : depResult.getRequirements()) {
        if (!req.isConsistentInBuild(this)) {
          executed = true;
          // Could get consistent because it was part of a cycle which is
          // compiled now
          if (requireStack.isConsistent(buildReq))
            return yield(buildReq, builder, depResult);
          Log.log.log("Rebuild because " + req + " not consistent", Log.DETAIL);
          return executeBuilder(builder, dep, buildReq);
        }
      }

      if (requireStack.areAllCyclicRequestsConsistent(buildReq)) {
        requireStack.markConsistent(buildReq);
      }

    } catch (RequiredBuilderFailed e) {
      if (executed || e.getLastAddedBuilder().getUnit().getPersistentPath().equals(depResult.getPersistentPath()))
        throw e;

      String desc = builder.description();
      if (desc != null)
        Log.log.log("Failing builder was required by \"" + desc + "\".", Log.CORE);
      throw e.enqueueBuilder(depResult, builder, false);
    } catch (BuildCycleException e) {
      Log.log.log("Build Cycle at " + dep + " init " + e.getCycleCause() + " rest " + e.getCycle().getCycleComponents(), Log.DETAIL);
      BuildCycle extendedCycle = requireStack.createCycleFor(buildReq);
      extendedCycle = new BuildCycle(e.getCycleCause(), extendedCycle.getCycleComponents());
      if (e.getCycleState() == CycleState.UNHANDLED && e.getCycle().getCycleComponents().contains(extendedCycle.getInitial())) {
        Log.log.log("Extend cycle to init " + extendedCycle.getInitial() + " rest " + extendedCycle.getCycleComponents(), Log.DETAIL);
        if (!extendedCycle.getCycleComponents().containsAll(e.getCycle().getCycleComponents())) {
          Log.log.log("Assert", Log.DETAIL);
          throw new AssertionError("Cycle " + e.getCycle().getCycleComponents() + " -  extended cycle " + extendedCycle.getCycleComponents());
        }
        throw new BuildCycleException(e.getMessage(), e.getCycleCause(), extendedCycle);
      } else {
        throw e;
      }
    } finally {
      requireStack.pop(buildReq);
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
