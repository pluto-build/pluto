package org.sugarj.cleardep.build;

import java.util.Arrays;
import java.util.List;

import org.sugarj.cleardep.build.BuildCycle.Result;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

public abstract class FixpointCycleSupport implements CycleSupport {

  private List<BuilderFactory<?, ?, ?>> supportedBuilders;

  public FixpointCycleSupport(BuilderFactory<?, ?, ?>... supportedBuilders) {
    this.supportedBuilders = Arrays.asList(supportedBuilders);
  }

  @Override
  public String getCycleDescription(BuildCycle cycle) {
    String cycleName = "Cycle ";
    for (BuildRequirement<?> req : cycle.getCycleComponents()) {
      cycleName += req.req.createBuilder().taskDescription();
    }
    return cycleName;
  }

  @Override
  public boolean canCompileCycle(BuildCycle cycle) {
    for (BuildRequirement<?> req : cycle.getCycleComponents()) {
      for (BuilderFactory<?, ?, ?> supportedBuilder : supportedBuilders) {
        if (req.req.factory == supportedBuilder){
          return true;
        }
      }
    }
    return true;
  }

  @Override
  public Result compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable {
    FixpointCycleBuildResultProvider cycleManager = new FixpointCycleBuildResultProvider(manager, cycle);

    int numInterations = 1;
    boolean cycleConsistent = false;
    while (!cycleConsistent) {
      boolean logStarted = false;
      cycleConsistent = true;
      try {
        // CycleComponents are in order if which they were required
        // Require the first one which is not consistent to their input
        for (BuildRequirement<?> req : cycle.getCycleComponents())
          if(!req.unit.isConsistentShallow(null)) {
            if (!logStarted) {
              Log.log.beginTask("Compile cycle iteration " + numInterations, Log.CORE);
              logStarted = true;
            }
            cycleConsistent = false;
            cycleManager.require(null,req.req);
          }

      } finally {
        if (logStarted) {
          Log.log.endTask();
        }
      }
      numInterations++;
      cycleManager.nextIteration();
    }
    Log.log.log("Fixpoint detected.", Log.CORE);
    return cycleManager.getResult();
  }

}
