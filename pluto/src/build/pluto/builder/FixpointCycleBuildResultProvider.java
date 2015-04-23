package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.CyclicBuildRequirement;
import build.pluto.output.Output;

public class FixpointCycleBuildResultProvider extends BuildUnitProvider {

  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  private Map<BuildRequest<?, ?, ?, ?>, Output> outputsPreviousIteration = new HashMap<>();

  private Map<BuildRequest<?, ?, ?, ?>, BuildUnit<?>> requiredUnitsInIteration = new HashMap<>();

  private BuildCycleResult result;

  public FixpointCycleBuildResultProvider(BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
    this.requiredUnitsInIteration = new HashMap<>();
    this.result = new BuildCycleResult();
  }

  public BuildCycleResult getResult() {
    return result;
  }

  public void nextIteration() {
    outputsPreviousIteration.clear();
    requiredUnitsInIteration.forEach((BuildRequest<?, ?, ?, ?> req, BuildUnit<?> unit) -> outputsPreviousIteration.put(req, unit.getBuildResult()));
    requiredUnitsInIteration.clear();
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
    BuildUnit<Out> cycleUnit = (BuildUnit<Out>) requiredUnitsInIteration.get(buildReq);
    @SuppressWarnings("unchecked")
    Out previousOutput = (Out) outputsPreviousIteration.get(buildReq);
    
    if (cycleUnit != null) {
      return new CyclicBuildRequirement<>(cycleUnit, buildReq, previousOutput);
    } else {
      
      if (cycle.getCycleComponents().contains(buildReq)) {

        Log.log.beginTask(buildReq.createBuilder().description(), Log.CORE);

        try {
          try {
            Builder<In, Out> builder = buildReq.createBuilder();
            File dep = builder.persistentPath();
            cycleUnit = BuildUnit.create(dep, buildReq);
            this.requiredUnitsInIteration.put(buildReq, cycleUnit);
            BuildManager.setUpMetaDependency(builder, cycleUnit);

            Out result = builder.triggerBuild(cycleUnit, this);
            cycleUnit.setBuildResult(result);
            cycleUnit.setState(BuildUnit.State.finished(true));

            this.result.setBuildResult(buildReq, result);
            return new BuildRequirement<Out>(cycleUnit, buildReq);

          } catch (BuildCycleException e) {
            Log.log.log("Stopped because of cycle", Log.CORE);
            throw this.tryCompileCycle(e);
          }
        } catch (BuildCycleException e2) {
          throw e2;
        } catch (Throwable e) {
          throw new RequiredBuilderFailed(new BuildRequirement<>(cycleUnit, buildReq), e);
        } finally {

          Log.log.endTask(cycleUnit.getState() == BuildUnit.State.SUCCESS);
        }
      } else {
        return this.parentManager.require(buildReq);
      }
    }
  }

  @Override
  protected Throwable tryCompileCycle(BuildCycleException e) {
    return this.parentManager.tryCompileCycle(e);
  }
}
