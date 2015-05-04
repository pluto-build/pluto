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

public class FixpointCycleBuildResultProvider extends BuildUnitProvider {

  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  private Map<BuildRequest<?, ?, ?, ?>, Output> outputsPreviousIteration = new HashMap<>();

  private Set<BuildUnit<?>> requiredUnitsInIteration;
  private Set<BuildUnit<?>> finishedUnitsInIteration;
  private Map<BuildRequest<?, ?, ?, ?>, BuildUnit<?>> units = new HashMap<>();

  public FixpointCycleBuildResultProvider(BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
    this.requiredUnitsInIteration = new HashSet<>();
    this.finishedUnitsInIteration = new HashSet<>();
    units = new HashMap<>();
  }

  public void nextIteration() {
    outputsPreviousIteration.clear();
    units.forEach((BuildRequest<?, ?, ?, ?> req, BuildUnit<?> unit) -> outputsPreviousIteration.put(req, unit.getBuildResult()));
    requiredUnitsInIteration.clear();
    finishedUnitsInIteration.clear();
  }

  private boolean isUnitCompletedInCurrentIteration(BuildUnit<?> unit) {
    return finishedUnitsInIteration.contains(unit);
  }

  private boolean isUnitRequiredInCurrentIteration(BuildUnit<?> unit) {
    return unit != null && requiredUnitsInIteration.contains(unit);
  }

  private boolean isBuildRequestPartOfCycle(BuildRequest<?, ?, ?, ?> req) {
    return cycle.getCycleComponents().contains(req);
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

    // Check whether the unit has already been required in the interation, then
    // do not compile it
    if (isUnitRequiredInCurrentIteration(cycleUnit)) {
      // If the unit is already completed, this is a normal build requirement,
      // other the units
      // requires itself transitivly, this a cyclic requirement
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

    Builder<In, Out> builder = buildReq.createBuilder();
    File dep = builder.persistentPath();


    // Local inconsistency check
    boolean noUnit = cycleUnit == null;
    InconsistenyReason inconsistent = noUnit ? null : cycleUnit.isConsistentNonrequirementsReason();
    boolean needBuild = noUnit || (inconsistent != InconsistenyReason.NO_REASON);

    if (!noUnit) {
      this.requiredUnitsInIteration.add(cycleUnit);
      this.units.put(buildReq, cycleUnit);
    }
    
    // Consistency check of requirements
    boolean depInconsistent = false;
    if (!needBuild && !noUnit) {
      for (Requirement req : cycleUnit.getRequirements()) {
        if (!req.isConsistentInBuild(this)) {
          depInconsistent = true;
          break;
        }
      }
    }

    needBuild = needBuild || depInconsistent;

    Log.log.log("Require " + buildReq.createBuilder().description() + " needs build: " + needBuild + ": " + (noUnit ? "no unit" : (inconsistent != InconsistenyReason.NO_REASON ? inconsistent : "")) + (depInconsistent ? "depInconsistent" : ""), Log.DETAIL);
    try {
      try {
        if (needBuild) {
          cycleUnit = BuildUnit.create(dep, buildReq);
          // Initialize the output to the old one, so units, which require this
          // units cylicly can work with the old result
          cycleUnit.setBuildResult(getOutputInPreviousIteration(buildReq));
          Log.log.beginTask(buildReq.createBuilder().description(), Log.CORE);
        }
        this.requiredUnitsInIteration.add(cycleUnit);
        this.units.put(buildReq, cycleUnit);

        if (needBuild) {

          BuildManager.setUpMetaDependency(builder, cycleUnit);

          Out result = builder.triggerBuild(cycleUnit, this);
          cycleUnit.setBuildResult(result);
          cycleUnit.setState(BuildUnit.State.finished(true));

        }
        finishedUnitsInIteration.add(cycleUnit);
        return new BuildRequirement<Out>(cycleUnit, buildReq);

      } catch (BuildCycleException e) {
        Log.log.log("Stopped because of cycle", Log.CORE);
        throw this.tryCompileCycle(e);
      }
    } catch (BuildCycleException e) {
      throw e;
    } catch (Throwable e) {
      cycleUnit.setState(State.FAILURE);
      throw new RequiredBuilderFailed(new BuildRequirement<>(cycleUnit, buildReq), e);
    } finally {
      if (needBuild)
        Log.log.endTask(cycleUnit.getState() == BuildUnit.State.SUCCESS);
    }
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    return this.parentManager.tryCompileCycle(e);
  }
}
