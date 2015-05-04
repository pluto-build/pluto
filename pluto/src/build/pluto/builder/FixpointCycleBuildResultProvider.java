package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.BuildUnit.State;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.CyclicBuildRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

/**
 * The {@link FixpointCycleBuildResultProvider} handles require calls for units
 * for the {@link FixpointCycleSupport}. During a iteration of compiling the
 * cycle a build request is compiled at most once. If it is required cyclicy the
 * already existing unit is used. To resolve cycles the output of the previous
 * iteration is used.
 * 
 * @author moritzlichter
 *
 */
public class FixpointCycleBuildResultProvider extends BuildUnitProvider {

  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  // Remember units for requests
  private Map<BuildRequest<?, ?, ?, ?>, BuildUnit<?>> units = new HashMap<>();
  // Remember the output of the previous iteration to resolve cyclic requests
  private Map<BuildRequest<?, ?, ?, ?>, Output> outputsPreviousIteration = new HashMap<>();

  // Track which units are required and completed in the current iteration
  private Set<BuildUnit<?>> requiredUnitsInIteration;
  private Set<BuildUnit<?>> completedUnitsInIteration;

  // Used to find the fixpoint: if no builder was executed in the iteration
  // because the cycle is consistent
  private boolean anyBuilderExecutedInIteration;

  /**
   * Creates a new {@link FixpointCycleBuildResultProvider} for the given cycle
   * and the given parent to resolve require calls which are not part of the
   * cycle
   * 
   * @param parentManager
   *          the parent {@link BuildUnitProvider}
   * @param cycle
   *          the cycle to compile
   */
  public FixpointCycleBuildResultProvider(BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
    this.requiredUnitsInIteration = new HashSet<>();
    this.completedUnitsInIteration = new HashSet<>();
    this.units = new HashMap<>();
  }

  /**
   * Starts a new iteration in fixpoint compiling.
   */
  protected void startNextIteration() {
    // Remember old outputs
    outputsPreviousIteration.clear();
    units.forEach((BuildRequest<?, ?, ?, ?> req, BuildUnit<?> unit) -> outputsPreviousIteration.put(req, unit.getBuildResult()));
    // Clear remembered units
    requiredUnitsInIteration.clear();
    completedUnitsInIteration.clear();
    anyBuilderExecutedInIteration = true;
  }

  private boolean isUnitCompletedInCurrentIteration(BuildUnit<?> unit) {
    return completedUnitsInIteration.contains(unit);
  }

  private void markUnitCompletedInCurrentIteration(BuildUnit<?> unit) {
    this.completedUnitsInIteration.add(unit);
  }

  private boolean isUnitRequiredInCurrentIteration(BuildUnit<?> unit) {
    return unit != null && requiredUnitsInIteration.contains(unit);
  }

  private <Out extends Output> void markUnitRequiredInCurrentInteration(BuildRequest<?, Out, ?, ?> req, BuildUnit<Out> unit) {
    requiredUnitsInIteration.add(unit);
    this.units.put(req, unit);
  }

  private boolean isBuildRequestPartOfCycle(BuildRequest<?, ?, ?, ?> req) {
    return cycle.getCycleComponents().contains(req);
  }

  protected boolean wasAnyBuilderExecutedInIteration() {
    return anyBuilderExecutedInIteration;
  }

  @SuppressWarnings("unchecked")
  private <Out> Out getOutputInPreviousIteration(BuildRequest<?, ?, ?, ?> req) {
    return (Out) outputsPreviousIteration.get(req);
  }

  @Override
  public
//@formatter:off
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
//@formatter:on
  BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    // Get the unit: either it has already been seen or read it
    @SuppressWarnings("unchecked")
    BuildUnit<Out> cycleUnit = (BuildUnit<Out>) units.get(buildReq);
    if (cycleUnit == null)
      cycleUnit = BuildUnit.read(buildReq.createBuilder().persistentPath());

    // Check whether the unit has already been required in the iteration, then
    // do not compile it
    if (isUnitRequiredInCurrentIteration(cycleUnit)) {
      // If the unit is already completed, this is a normal build requirement,
      // other the units
      // requires itself transitively, this a cyclic requirement
      if (isUnitCompletedInCurrentIteration(cycleUnit))
        return new BuildRequirement<>(cycleUnit, buildReq);
      else
        return new CyclicBuildRequirement<>(cycleUnit, buildReq, getOutputInPreviousIteration(buildReq));
    } else {
      // If the request is part of the cycle, we need to handle it, otherwise
      // just delegate it to the parent
      if (isBuildRequestPartOfCycle(buildReq))
        return requireInCycle(buildReq, cycleUnit);
      else
        return this.parentManager.require(buildReq);
    }
  }

  private
//@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
//@formatter:on
  BuildRequirement<Out> requireInCycle(BuildRequest<In, Out, B, F> buildReq, BuildUnit<Out> cycleUnit) throws IOException {
    // Local inconsistency check
    boolean noUnit = cycleUnit == null;
    if (noUnit) {
      Log.log.log("Require " + buildReq.createBuilder().description() + " needs build because no unit is found", Log.DETAIL);
      return executeInCycle(buildReq);
    }

    InconsistenyReason inconsistent = cycleUnit.isConsistentNonrequirementsReason();
    final boolean localInconsistent = inconsistent != InconsistenyReason.NO_REASON;
    if (localInconsistent) {
      Log.log.log("Require " + buildReq.createBuilder().description() + " need build because locally inconsistent: " + inconsistent, Log.DETAIL);
      return executeInCycle(buildReq);
    }

    // Remember that unit has been required, because consistency checks of
    // requirements may require this unit again
    markUnitRequiredInCurrentInteration(buildReq, cycleUnit);

    // Consistency check of requirements
    for (Requirement req : cycleUnit.getRequirements()) {
      if (!req.isConsistentInBuild(this)) {
        Log.log.log("Require " + buildReq.createBuilder().description() + " needs build because requirement is not consistent: " + req, Log.DETAIL);
        return executeInCycle(buildReq);
      }
    }

    // Now unit is consistent
    markUnitCompletedInCurrentIteration(cycleUnit);
    return new BuildRequirement<Out>(cycleUnit, buildReq);
  }

  private
//@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
//@formatter:on
  BuildRequirement<Out> executeInCycle(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    anyBuilderExecutedInIteration = true;
    Builder<In, Out> builder = buildReq.createBuilder();
    File dep = builder.persistentPath();
    BuildUnit<Out> cycleUnit = BuildUnit.create(dep, buildReq);

    try {
      try {
        // Initialize the output to the old one, so units, which require this
        // units cylicly can work with the old result
        cycleUnit.setBuildResult(getOutputInPreviousIteration(buildReq));
        Log.log.beginTask(buildReq.createBuilder().description(), Log.CORE);

        markUnitRequiredInCurrentInteration(buildReq, cycleUnit);

        BuildManager.setUpMetaDependency(builder, cycleUnit);

        // Trigger the build
        Out result = builder.triggerBuild(cycleUnit, this);
        cycleUnit.setBuildResult(result);
        cycleUnit.setState(BuildUnit.State.finished(true));

        markUnitCompletedInCurrentIteration(cycleUnit);
        return new BuildRequirement<Out>(cycleUnit, buildReq);
      } catch (BuildCycleException e) {
        Log.log.log("Stopped because of cycle", Log.CORE);
        throw this.tryCompileCycle(e);
      }
    } catch (BuildCycleException e) {
      // Build CycleException are delegated to parent unit provider
      throw e;
    } catch (Throwable e) {
      // Build exception
      cycleUnit.setState(State.FAILURE);
      throw new RequiredBuilderFailed(new BuildRequirement<>(cycleUnit, buildReq), e);
    } finally {
      Log.log.endTask(cycleUnit.getState() == BuildUnit.State.SUCCESS);
    }
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    return this.parentManager.tryCompileCycle(e);
  }
}
