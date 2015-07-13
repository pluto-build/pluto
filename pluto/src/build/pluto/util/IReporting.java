package build.pluto.util;

import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public interface IReporting {

  public static enum BuildReason {
    NoBuildSummary, ChangedBuilderInput, 
    BuildSummaryChanged, PreviousBuildNotFinished, InconsistentProvidedFiles,
    ExpiredOutput, InconsistentRequirement,
    FixPointNotReachedYet;
    
    public static BuildReason from(InconsistenyReason inc) {
      switch (inc) {
      case FILES_NOT_CONSISTENT: return InconsistentProvidedFiles;
      case PERSISTENT_VERSION_CHANGED: return BuildSummaryChanged;
      case NOT_FINISHED: return PreviousBuildNotFinished;
      default: throw new IllegalArgumentException(inc.toString());
      }
    }
  }
  
  public <O extends Output> void buildRequirement(BuildRequest<?, O, ?, ?> req);
  public <O extends Output> void finishedBuildRequirement(BuildRequest<?, O, ?, ?> req);
  
  public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons);
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void skippedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  
  public <O extends Output> void canceledBuilderFailure(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void canceledBuilderException(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, Throwable t);
  public <O extends Output> void canceledBuilderInterrupt(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e);

  public void startBuildCycle(BuildCycle cycle, CycleSupport cycleSupport);
  public void finishedBuildCycle(BuildCycle cycle, CycleSupport cycleSupport, Set<BuildUnit<?>> units);
  public void cancelledBuildCycleException(BuildCycle cycle, CycleSupport cycleSupport, Throwable t);
  
  public void inconsistentRequirement(Requirement req);
  
  public void messageFromBuilder(String message, boolean isError, Builder<?, ?> from);
  
  /**
   * @param message
   * @param isError
   * @param from
   * @param verbosity ranges from 0 (essential) to 10 (optional technical detail)
   */
  public void messageFromSystem(String message, boolean isError, int verbosity);
}
