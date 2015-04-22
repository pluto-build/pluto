package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildCycle.Result;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.CyclicBuildRequirement;
import build.pluto.output.Output;
import build.pluto.util.AbsoluteComparedFile;

public class FixpointCycleBuildResultProvider extends BuildUnitProvider {

  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  private Map<BuildUnit<?>, Output> requiredUnitsInIteration;

  private Result result;

  public FixpointCycleBuildResultProvider(BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
    this.requiredUnitsInIteration = new HashMap<>();
    this.result = new Result();
  }

  public Result getResult() {
    return result;
  }

  public void nextIteration() {
    requiredUnitsInIteration.clear();
  }

  private <In extends Serializable, Out extends Output, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildUnit<Out> getBuildUnitInCycle(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    File depPath = buildReq.createBuilder().persistentPath();
    for (BuildRequirement<?> req : this.cycle.getCycleComponents()) {
      if (AbsoluteComparedFile.equals(req.getUnit().getPersistentPath(), depPath)) {
        return (BuildUnit<Out>) req.getUnit();
      }
    }
    return null;
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
    
    BuildUnit<Out> cycleUnit = getBuildUnitInCycle(buildReq);
    if (cycleUnit != null && (this.requiredUnitsInIteration.containsKey(cycleUnit))) {
      @SuppressWarnings("unchecked")
      Out previousOutput = (Out) this.requiredUnitsInIteration.get(cycleUnit);
      return new CyclicBuildRequirement<>(cycleUnit, buildReq, previousOutput);
    } else {
      if (cycleUnit != null) {

        this.requiredUnitsInIteration.put(cycleUnit, cycleUnit.getBuildResult());
        
        if (cycleUnit.isConsistentShallow(null)) {
          return new BuildRequirement<>(cycleUnit, buildReq);
        }

        Log.log.beginTask(buildReq.createBuilder().description(), Log.CORE);

        try {
          try {
            Builder<In, Out> builder = buildReq.createBuilder();
            File dep =  builder.persistentPath();
            cycleUnit = BuildUnit.create(dep, buildReq);
            BuildManager.setUpMetaDependency(builder, cycleUnit);

            Out result = builder.triggerBuild(cycleUnit, this);
            cycleUnit.setBuildResult(result);
            cycleUnit.setState(State.finished(true));

            this.result.setBuildResult(cycleUnit, result);
            return new BuildRequirement<>(cycleUnit, buildReq);

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
