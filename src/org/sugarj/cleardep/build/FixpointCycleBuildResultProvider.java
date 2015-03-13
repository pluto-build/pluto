package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildCycle.Result;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class FixpointCycleBuildResultProvider implements BuildUnitProvider {

  private FixpointCycleSupport parent;
  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  private Set<BuildUnit<?>> requiredUnitsInIteration;

  private Result result;

  public FixpointCycleBuildResultProvider(FixpointCycleSupport parent, BuildUnitProvider parentManager, BuildCycle cycle) {
    super();
    this.parentManager = parentManager;
    this.cycle = cycle;
    this.requiredUnitsInIteration = new HashSet<>();
    this.parent = parent;
    this.result = new Result();
  }

  public Result getResult() {
    return result;
  }

  public void nextIteration() {
    requiredUnitsInIteration.clear();
  }

  private <In extends Serializable, Out extends Serializable, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildUnit<Out> getBuildUnitInCycle(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    Path depPath = buildReq.createBuilder().persistentPath();
    for (BuildRequirement<?> req : this.cycle.getCycleComponents()) {
      if (req.unit.getPersistentPath().equals(depPath)) {
        return (BuildUnit<Out>) req.unit;
      }
    }
    return null;
  }

  @Override
  public <In extends Serializable, Out extends Serializable, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildUnit<Out> require(BuildUnit<?> source, BuildRequest<In, Out, B, F> buildReq) throws IOException {
    BuildUnit<Out> cycleUnit = getBuildUnitInCycle(buildReq);
    if (cycleUnit != null && (source == cycleUnit || this.requiredUnitsInIteration.contains(cycleUnit))) {
      return cycleUnit;
    } else {
      if (cycleUnit != null) {
        this.requiredUnitsInIteration.add(cycleUnit);
        Log.log.beginTask(this.parent.getBuilderForInput(buildReq.input).taskDescription(buildReq.input), Log.CORE);
        try {

          Out result = (Out) this.parent.getBuilderForInput(buildReq.input).compileRequest(this, (BuildRequest) buildReq);
          cycleUnit.setBuildResult(result);
          this.result.setBuildResult(cycleUnit, result);
          return cycleUnit;
        } catch (BuildCycleException e) {
          Log.log.log("Stopped because of cycle", Log.CORE);
          try {
            this.parentManager.tryCompileCycle(e);
          } catch (BuildCycleException e2) {
            throw e2;
          } catch (Throwable e2) {
            throw new RuntimeException(e2);
          }
          throw e;
        } catch (Throwable e) {
          throw new RequiredBuilderFailed(buildReq.factory.makeBuilder(buildReq.input), cycleUnit, e);
        } finally {

          Log.log.endTask();
        }
      } else {
        return this.parentManager.require(source, buildReq);
      }
    }
  }

  @Override
  public void tryCompileCycle(BuildCycleException e) throws Throwable {
    this.parentManager.tryCompileCycle(e);
  }

}
