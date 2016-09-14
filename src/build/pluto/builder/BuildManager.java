package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.sugarj.common.Exec;

import com.cedarsoftware.util.DeepEquals;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildCycleException.CycleState;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.dependency.database.MultiMapDatabase;
import build.pluto.dependency.database.XodusDatabase;
import build.pluto.output.Output;
import build.pluto.util.IReporting;
import build.pluto.util.IReporting.BuildReason;

public class BuildManager extends BuildUnitProvider implements AutoCloseable {
  public static boolean ASSERT_SERIALIZABLE = false;

  private ExecutingStack executingStack;
  private transient RequireStack requireStack;

  private static Map<Thread, Long> requireInitiallyTimestamps = new HashMap<>();

  public BuildManager(IReporting report) {
    this(report, "shared");
  }

  public BuildManager(IReporting report, String path) {
    super(report, new DynamicAnalysis(report, XodusDatabase.createFileDatabase(path)));
    this.executingStack = new ExecutingStack();
    this.requireStack = new RequireStack();
  }

  public BuildManager(IReporting report, MultiMapDatabase<File, File> genBy) {
    super(report, new DynamicAnalysis(report, genBy));
    this.executingStack = new ExecutingStack();
    this.requireStack = new RequireStack();
  }

  private <Out extends Output> void checkInterrupt(boolean duringRequire, File dep, BuildUnit<Out> depResult, BuildRequest<?, Out, ?, ?> buildReq) throws IOException {
    if (Thread.interrupted()) {
      if (depResult == null)
        depResult = BuildUnit.create(dep, buildReq);
      if (!duringRequire) {
        depResult.requireOther(Requirement.FALSE);
        depResult.setState(BuildUnit.State.FAILURE);
      }
      report.canceledBuilderInterrupt(buildReq, depResult);
      throw RequiredBuilderFailed.init(new BuildRequirement<Out>(depResult, buildReq), new InterruptedException("Build was interrupted"));
    }
  }

  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildRequirement<Out> executeBuilder(Builder<In, Out> builder, File dep, BuildRequest<In, Out, B, F> buildReq, Set<BuildReason> reasons) throws IOException {

    this.requireStack.beginRebuild(buildReq, reasons);

    BuildUnit<Out> depResult = BuildUnit.read(dep);
    BuildUnit<Out> previousDepResult = depResult == null ? null : depResult.clone();

    dynamicAnalysis.reset(depResult);
    report.startedBuilder(buildReq, builder, depResult, reasons);

    depResult = BuildUnit.create(dep, buildReq);

    setUpMetaDependency(builder, depResult);

    // First step: cycle detection
    this.executingStack.push(buildReq);

    int inputHash = DeepEquals.deepHashCode(builder.getInput());

    depResult.setState(BuildUnit.State.IN_PROGESS);
    boolean regularFinish = false;

    try {
      try {
        // call the actual builder
        Out out = builder.triggerBuild(depResult, this, previousDepResult);
        depResult.setBuildResult(out);
        if (!depResult.isFinished())
          depResult.setState(BuildUnit.State.SUCCESS);
        regularFinish = true;
      } catch (BuildCycleException e) {
        throw this.tryCompileCycle(e);
      }
    } catch (BuildCycleException e) {
      report.canceledBuilderCycle(buildReq, depResult, e);
      stopBuilderInCycle(builder, buildReq, depResult, inputHash, e);

    } catch (RequiredBuilderFailed e) {
      report.canceledBuilderRequiredBuilderFailed(buildReq, depResult, e);
      throw e.enqueueBuilder(depResult, buildReq);

    } catch (ClosedByInterruptException e) {
      if (!Thread.currentThread().isInterrupted())
        Thread.interrupted();
      // triggers regular interrupt handler below

    } catch (Throwable e) {
      report.canceledBuilderException(buildReq, depResult, e);
      throw RequiredBuilderFailed.init(new BuildRequirement<Out>(depResult, buildReq), e);

    } finally {
      if (!depResult.isFinished())
        depResult.setState(BuildUnit.State.FAILURE);

      this.executingStack.pop(buildReq);
      this.requireStack.finishRebuild(buildReq);

      try {
        dynamicAnalysis.check(depResult, inputHash);
        checkInterrupt(false, dep, depResult, buildReq); // interrupt before
                                                         // consistency
                                                         // assertion because an
                                                         // interrupted build is
                                                         // never consistent.
        assertConsistency(depResult);
      } finally {
        report.messageFromSystem("Wrote " + dep, false, 10);
        depResult.write();
      }

      if (regularFinish && depResult.getState() == BuildUnit.State.SUCCESS)
        report.finishedBuilder(buildReq, depResult);
      else if (regularFinish && depResult.getState() == BuildUnit.State.FAILURE) {
        report.canceledBuilderFailure(buildReq, depResult);
        throw new RequiredBuilderFailed(new BuildRequirement<Out>(depResult, buildReq), new Error("Builder failed"));
      }
    }

    return new BuildRequirement<Out>(depResult, buildReq);
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    // Only try to compile a cycle which is unhandled
    if (e.getCycleState() != CycleState.UNHANDLED) {
      return e;
    }

    report.messageFromSystem("Detected a dependency cycle with root " + e.getCycleCause().createBuilder().persistentPath(), false, 0);

    e.setCycleState(CycleState.NOT_RESOLVED);
    BuildCycle cycle = e.getCycle();
    CycleHandler cycleSupport = cycle.findCycleSupport();
    if (cycleSupport == null)
      throw e;

    report.startBuildCycle(cycle, cycleSupport);
    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents())
      requireStack.push(req);

    Set<BuildUnit<?>> resultUnits = null;
    try {
      resultUnits = cycleSupport.buildCycle(this);
      for (BuildUnit<?> resultUnit : resultUnits) {
        resultUnit.write();
        this.requireStack.markConsistent(resultUnit.getGeneratedBy());
      }

      e.setCycleState(CycleState.RESOLVED);
      return e;
    } catch (BuildCycleException cyclicEx) {
      // New cycle in cycle detected, use result from it
      // But keep throw away the new exception but use
      // the existing ones to kill all builders of this
      // cycle
      e.setCycleState(cyclicEx.getCycleState());
      throw cyclicEx;
    } catch (Throwable t) {
      e.setCycleState(CycleState.RESOLVED);
      report.cancelledBuildCycleException(cycle, cycleSupport, t);
      return t;
    } finally {
      for (int i = cycle.getCycleComponents().size() - 1; i >= 0; i--) {
        requireStack.pop(cycle.getCycleComponents().get(i));
      }
      report.finishedBuildCycle(cycle, cycleSupport, resultUnits);
    }
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

      dynamicAnalysis.check(depResult, inputHash);
    } else {
      depResult.setState(State.FAILURE);
    }

    if (e.isFirstInvokedOn(buildReq)) {
      if (e.getCycleState() != CycleState.RESOLVED) {
        // Cycle cannot be handled
        report.cancelledBuildCycleException(e.getCycle(), null, e);
        throw new RequiredBuilderFailed(new BuildRequirement<Out>(depResult, buildReq), e);
      } else {

        if (this.executingStack.getNumContains(e.getCycleCause()) != 1) {
          report.messageFromSystem("Too many entries of cycle cause left in execution stac", true, 0);
          report.cancelledBuildCycleException(e.getCycle(), null, e);
          throw e;
        }
      }

    } else {
      // Kill depending builders
      throw e;
    }
  }

  //@formatter:off
  public
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> requireInitially(BuildRequest<In, Out, B, F> buildReq) throws Throwable {
    try {
      Thread currentThread = Thread.currentThread();
      long currentTime = System.currentTimeMillis();
      requireInitiallyTimestamps.put(currentThread, currentTime);
      report.messageFromSystem("Incrementally rebuild inconsistent units", false, 0);
      BuildRequirement<Out> result = require(buildReq, true);
      return result.getUnit();
    } catch (RequiredBuilderFailed e) {
      Throwable cause = e.getCause();
      if (cause != null)
        throw cause;
      else
        throw e;
    } finally {
      Exec.shutdown();
    }
  }

  public static long getStartingTimeOfBuildManager(Thread thread) {
    Long l = requireInitiallyTimestamps.get(thread);
    return l == null ? 0l : l;
  }

  @Override
  //@formatter:off
  public
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildRequirement<Out> require(final BuildRequest<In, Out, B, F> buildReq, boolean needBuildResult) throws IOException {

    B builder = buildReq.createBuilder();

    report.buildRequirement(buildReq);

    File dep = builder.persistentPath();
    BuildUnit<Out> depResult = BuildUnit.read(dep);

    checkInterrupt(true, dep, depResult, buildReq);

    // Dont execute require because it is cyclic, requireStack keeps track of
    // this

    // Need to check that before putting dep on the requires Stack because
    // otherwise dep has always been required
    boolean alreadyRequired = requireStack.push(buildReq);
    boolean executed = false;
    try {
      if (alreadyRequired) {
        report.messageFromSystem("Already required " + buildReq, false, 7);
        boolean assumptionIncomplete = requireStack.existsInconsistentCyclicRequest(buildReq);
        report.messageFromSystem("Assumptions inconsistent " + assumptionIncomplete, false, 10);
        if (!assumptionIncomplete) {
          return yield(buildReq, builder, depResult);
        } else {
          report.messageFromSystem("Deptected Require cycle for " + dep, false, 7);
          BuildCycle cycle = requireStack.createCycleFor(buildReq);
          BuildRequest<?, ?, ?, ?> cycleCause = executingStack.topMostEntry(cycle.getCycleComponents());

          cycle = new BuildCycle(cycleCause, cycle.getCycleComponents());
          BuildCycleException ex = new BuildCycleException("Require build cycle " + cycle.getCycleComponents().size() + " on " + dep, cycleCause, cycle);
          throw ex;
        }
      }

      if (depResult != null && requireStack.isConsistent(buildReq))
        return yield(buildReq, builder, depResult);

      Set<BuildReason> reasons = computeLocalBuildReasons(buildReq, needBuildResult, dep, depResult);

      if (!reasons.isEmpty()) {
        // Local inconsistency should execute the builder regardless whether it
        // has been required to detect the cycle
        // TODO should inconsistent file requirements trigger the same, they
        // should i think
        executed = true;
        return executeBuilder(builder, dep, buildReq, reasons);
      }

      for (Requirement req : depResult.getRequirements()) {
        if (!req.tryMakeConsistent(this)) {
          executed = true;
          // Could get consistent because it was part of a cycle which is
          // compiled now
          if (requireStack.isConsistent(buildReq))
            return yield(buildReq, builder, depResult);

          report.inconsistentRequirement(req);
          reasons.add(BuildReason.InconsistentRequirement);
          return executeBuilder(builder, dep, buildReq, reasons);
        }
      }

      // Note: This handles the non cyclic case too, then all other cyclic
      // requests are empty, thus all assumed

      // Checks whether all other cyclic requests are assumed to be consistent,
      // which means, that all of them reached this point
      // already, thus checked all assumptions
      if (requireStack.areAllOtherCyclicRequestsAssumed(buildReq)) {
        // If yes, together with this unit, all cyclic requests are checked and
        // the cycle is consistent
        requireStack.markAllConsistent(buildReq);
      } else {
        // No, then only mark this as assummed
        requireStack.markAssumed(buildReq);
      }

      report.skippedBuilder(buildReq, depResult);
    } catch (RequiredBuilderFailed e) {
      if (executed || e.getLastAddedBuilder().getUnit().getPersistentPath().equals(depResult.getPersistentPath()))
        throw e;

      String desc = builder.description();
      if (desc != null)
        report.messageFromSystem("Failing builder was required by \"" + desc + "\".", true, 0);
      throw e.enqueueBuilder(depResult, buildReq, false);

    } catch (BuildCycleException e) {
      report.messageFromSystem("Build Cycle at " + dep + " init " + e.getCycleCause() + " rest " + e.getCycle().getCycleComponents(), false, 7);
      BuildCycle extendedCycle = requireStack.createCycleFor(buildReq);
      extendedCycle = new BuildCycle(e.getCycleCause(), extendedCycle.getCycleComponents());

      if (e.getCycleState() == CycleState.UNHANDLED && e.getCycle().getCycleComponents().contains(extendedCycle.getInitial())) {
        report.messageFromSystem("Extend cycle to init " + extendedCycle.getInitial() + " rest " + extendedCycle.getCycleComponents(), false, 7);
        if (!extendedCycle.getCycleComponents().containsAll(e.getCycle().getCycleComponents()))
          throw new AssertionError("Cycle " + e.getCycle().getCycleComponents() + " -  extended cycle " + extendedCycle.getCycleComponents());
        throw new BuildCycleException(e.getMessage(), e.getCycleCause(), extendedCycle);
      } else {
        throw e;
      }

    } finally {
      requireStack.pop(buildReq);
      report.finishedBuildRequirement(buildReq);
    }

    return yield(buildReq, builder, depResult);
  }

  public void resetDynamicAnalysis() throws IOException {
    dynamicAnalysis.resetAnalysis();
  }

  @Override
  public void close() throws IOException {
    dynamicAnalysis.close();
  }

  private <In extends Serializable, Out extends Output, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> Set<BuildReason> computeLocalBuildReasons(final BuildRequest<In, Out, B, F> buildReq, boolean needBuildResult, File dep, BuildUnit<Out> depResult) {
    Set<BuildReason> reasons = new TreeSet<IReporting.BuildReason>();

    Set<BuildReason> knownInconsistent = requireStack.isKnownInconsistent(dep);
    if (knownInconsistent != null)
      reasons.addAll(knownInconsistent);

    if (depResult == null) {
      report.messageFromSystem("No result unit found", false, 10);
      reasons.add(BuildReason.NoBuildSummary);
    } else {
      if (!depResult.getGeneratedBy().deepEquals(buildReq)) {
        report.messageFromSystem("Builder Input changed", false, 10);
        reasons.add(BuildReason.ChangedBuilderInput);
      }

      InconsistenyReason localInconsistencyReason = depResult.isConsistentNonrequirementsReason();
      if (localInconsistencyReason != InconsistenyReason.NO_REASON) {
        report.messageFromSystem("Local inconsistent " + localInconsistencyReason, false, 10);
        reasons.add(BuildReason.from(localInconsistencyReason));
      }

      boolean noOut = !(depResult.getBuildResult() instanceof build.pluto.output.Out<?>);
      boolean expiredOutput = noOut ? false : ((build.pluto.output.Out<?>) depResult.getBuildResult()).expired();
      if (needBuildResult && expiredOutput) {
        report.messageFromSystem("Expired output", false, 10);
        reasons.add(BuildReason.ExpiredOutput);
      }
    }

    return reasons;
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
      report.messageFromBuilder(e.getMessage(), true, builder);
      throw e;
    }
    return new BuildRequirement<>(unit, req);
  }

  private <Out extends Output> void assertConsistency(BuildUnit<Out> depResult) {
    assert depResult.isConsistentShallowReason() == InconsistenyReason.NO_REASON : "Build manager does not guarantee soundness, got consistency status " + depResult.isConsistentShallowReason() + " for " + depResult.getPersistentPath();
  }
}
