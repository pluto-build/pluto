package build.pluto.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;

public class FixpointCycleSupport implements CycleSupport {

  private List<BuilderFactory<?, ?, ?>> supportedBuilders;

  public FixpointCycleSupport(BuilderFactory<?, ?, ?>... supportedBuilders) {
    this.supportedBuilders = Arrays.asList(supportedBuilders);
  }

  @Override
  public String getCycleDescription(BuildCycle cycle) {
    String cycleName = "Fixpoint {";
    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      cycleName += req.createBuilder().description() + ";";
    }
    cycleName = cycleName.substring(0, cycleName.length() - 1) + "}";
    return cycleName;
  }

  @Override
  public boolean canCompileCycle(BuildCycle cycle) {
    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      for (BuilderFactory<?, ?, ?> supportedBuilder : supportedBuilders) {
        if (req.factory == supportedBuilder) {
          return true;
        }
      }
    }
    return true;
  }

  @Override
  public BuildCycleResult compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable {
    FixpointCycleBuildResultProvider cycleManager = new FixpointCycleBuildResultProvider(manager, cycle);

    int numInterations = 1;
    boolean cycleConsistent = false;
    Map<BuildRequest<?, ?, ?, ?>, BuildUnit<?>> cycleUnits = new HashMap<>();
    while (!cycleConsistent) {
      Log.log.log("Begin interation " + numInterations,  Log.CORE);
      // Log.log.log("Cycle " +
      // cycle.getCycleComponents().stream().map((BuildRequest r) ->
      // r.createBuilder().description()).collect(Collectors.toList()),
      // Log.CORE);
      boolean logStarted = false;
      cycleConsistent = true;// !cycleUnits.isEmpty() &&
                             // cycleUnits.values().stream().map(BuildUnit::isConsistentShallow).reduce(true,
                             // Boolean::logicalAnd);
      try {
        // CycleComponents are in order if which they were required
        // Require the first one which is not consistent to their input
        for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
          final BuildUnit<?> unit = cycleUnits.get(req);
          // Log.log.log("Require " + req.createBuilder().description(),
          // Log.CORE);
          // Check whether the unit is shallowly consistent (if null its the
          // first iteration)
          if (unit == null || !unit.isConsistentShallow()) {
            if (!logStarted) {
              Log.log.beginTask("Compile cycle iteration " + numInterations, Log.CORE);
              logStarted = true;
            }
            cycleConsistent = false;
            final BuildRequirement<?> newUnit = cycleManager.require(req);
            cycleUnits.put(req, newUnit.getUnit());
          }
        }
      } finally {
        if (logStarted) {
          Log.log.endTask();
        }
      }
      Log.log.log("End iteration " + numInterations, Log.CORE);
      numInterations++;
      cycleManager.nextIteration();
    }
    Log.log.log("Fixpoint detected.", Log.CORE);
    return cycleManager.getResult();
  }

}
