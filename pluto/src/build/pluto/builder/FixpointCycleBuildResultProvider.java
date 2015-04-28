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

  @Override
  public
//@formatter:off
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
//@formatter:on
  BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    @SuppressWarnings("unchecked")
    BuildUnit<Out> cycleUnit = (BuildUnit<Out>) units.get(buildReq);
    if (cycleUnit == null)
      cycleUnit = BuildUnit.read(buildReq.createBuilder().persistentPath());
    @SuppressWarnings("unchecked")
    Out previousOutput = (Out) outputsPreviousIteration.get(buildReq);
    if (cycleUnit != null && requiredUnitsInIteration.contains(cycleUnit)) {
      if (finishedUnitsInIteration.contains(cycleUnit)) {
        return new BuildRequirement<>(cycleUnit, buildReq);
      }
      return new CyclicBuildRequirement<>(cycleUnit, buildReq, previousOutput);
    } else {

      if (cycle.getCycleComponents().contains(buildReq)) {
        // Log.log.log("In cycle compile", Log.CORE);

        Builder<In, Out> builder = buildReq.createBuilder();
        File dep = builder.persistentPath();

        // TODO this overapproximates: first check inconsistency without
        // dependencies
        // then replay the dependencies and finally check again whether after
        // requiring
        // the dependencies the unit is consistent again (very similiar to
        // require in
        // BuildManager)

        boolean noUnit = cycleUnit == null;
        InconsistenyReason inconsistent = cycleUnit == null ? null : cycleUnit.isConsistentNonrequirementsReason();

        boolean needBuild = noUnit || (inconsistent != InconsistenyReason.NO_REASON);

        if (cycleUnit != null) {

          this.requiredUnitsInIteration.add(cycleUnit);
          this.units.put(buildReq, cycleUnit);
        }
        boolean depInconsistent = false;

        if (!needBuild && cycleUnit != null) {
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
              cycleUnit.setBuildResult(previousOutput);
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
        } catch (BuildCycleException e2) {
          throw e2;
        } catch (Throwable e) {
          cycleUnit.setState(State.FAILURE);
          throw new RequiredBuilderFailed(new BuildRequirement<>(cycleUnit, buildReq), e);
        } finally {
          if (needBuild)
            Log.log.endTask(cycleUnit.getState() == BuildUnit.State.SUCCESS);
        }
      } else {
        Log.log.log("Require parent", Log.CORE);
        return this.parentManager.require(buildReq);
      }
    }
  }
  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    return this.parentManager.tryCompileCycle(e);
  }
}
