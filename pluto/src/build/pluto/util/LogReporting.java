package build.pluto.util;

import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public class LogReporting implements IReporting {

  private Log log = Log.log;
  
  @Override
  public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons) {
    String desc = b.description();
    log.beginTask(desc, Log.CORE);
  }

  @Override
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    log.endTask(unit.getState() == BuildUnit.State.SUCCESS);
  }

  @Override
  public <O extends Output> void skippedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    // nothing
  }
  
  @Override
  public void inconsistentRequirement(Requirement req) {
    // nothing
  }


  @Override
  public void messageFromBuilder(String message, boolean isError, Builder<?, ?> from) {
    if (isError)
      log.logErr(message, Log.CORE);
    else
      log.log(message, Log.CORE);
  }

  @Override
  public void messageFromSystem(String message, boolean isError, int verbosity) {
    if (verbosity <= 3)
      log.log(message, Log.CORE);
    else
      log.log(message, Log.DETAIL);
  }

  @Override
  public <O extends Output> void canceledBuilderFailure(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    log.endTask("Builder failed for unknown reason, please confer log.");
  }
  
  @Override
  public <O extends Output> void canceledBuilderInterrupt(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    log.endTask("Build was interrupted");
  }

  @Override
  public <O extends Output> void canceledBuilderException(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, Throwable t) {
    log.endTask(t.getClass() + (t == null ? "" : (": " + t.getMessage())));
    if (t instanceof BuildCycleException)
      new RuntimeException("Unexpected build cycle ex").printStackTrace();
    else
      t.printStackTrace();
  }
  
  public <O extends Output> void canceledBuilderCycle(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, BuildCycleException t) {
    log.endTask("Cycle detected. Stopping running builders before building cycle.");
  }

  @Override
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e) {
    log.endTask(e.getMessage());
  }

  @Override
  public void startBuildCycle(BuildCycle cycle, CycleSupport cycleSupport) {
    log.beginTask("Build cycle with: " + cycleSupport.cycleDescription(), Log.CORE);
  }

  @Override
  public void finishedBuildCycle(BuildCycle cycle, CycleSupport cycleSupport, Set<BuildUnit<?>> units) {
    log.endTask();
  }
  
  @Override
  public void cancelledBuildCycleException(BuildCycle cycle, CycleSupport cycleSupport, Throwable t) {
    if (cycleSupport == null && t instanceof BuildCycleException)
      log.endTask("Could not find cycle support for cycle: " + cycle.getCycleComponents());
    else
      log.endTask("Cycle building failed: " + t.getMessage());
  }

  @Override
  public <O extends Output> void buildRequirement(BuildRequest<?, O, ?, ?> req) {
    // nothing
  }

  @Override
  public <O extends Output> void finishedBuildRequirement(BuildRequest<?, O, ?, ?> req) {
    // nothing
  }
}
