package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;

public class FixpointCycleBuildResultProvider extends BuildUnitProvider {

  private BuildUnitProvider parentManager;

  private BuildCycle cycle;

  // private Set<BuildRequest<?, ?, ?, ?>> requiredUnitsInIteration;
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
    requiredUnitsInIteration.clear();
  }

  /*
   * @SuppressWarnings("unchecked") private <In extends Serializable, Out
   * extends Serializable, B extends Builder<In, Out>, F extends
   * BuilderFactory<In, Out, B>> BuildUnit<Out>
   * getBuildUnitInCycle(BuildRequest<In, Out, B, F> buildReq) throws
   * IOException { File depPath = buildReq.createBuilder().persistentPath(); for
   * (BuildRequest<?, ?, ?, ?> req : this.cycle.getCycleComponents()) { if
   * (AbsoluteComparedFile.equals(req.getUnit().getPersistentPath(), depPath)) {
   * return (BuildUnit<Out>) req.getUnit(); } } return null; }
   */

  @Override
  public
//@formatter:off
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
//@formatter:on
  BuildUnit<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {

    BuildUnit<Out> cycleUnit = (BuildUnit<Out>) requiredUnitsInIteration.get(buildReq);
    if (cycleUnit != null) {
      return cycleUnit;
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
            cycleUnit.setState(State.finished(true));

            this.result.setBuildResult(buildReq, result);
            return cycleUnit;

          } catch (BuildCycleException e) {
            Log.log.log("Stopped because of cycle", Log.CORE);
            throw this.tryCompileCycle(e);
          }
        } catch (BuildCycleException e2) {
          throw e2;
        } catch (Throwable e) {
          throw new RequiredBuilderFailed(buildReq.factory.makeBuilder(buildReq.input), cycleUnit, e);
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
