package build.pluto.util;

import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.InconsistenyReason;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public interface IReporting {

  public static enum BuildReason {
    NoBuildSummary, ChangedBuilderInput, 
    BuildSummaryChanged, PreviousBuildNotFinished, InconsistentProvidedFiles,
    ExpiredOutput, InconsistentRequirement;
    
    public static BuildReason from(InconsistenyReason inc) {
      switch (inc) {
      case FILES_NOT_CONSISTENT: return InconsistentProvidedFiles;
      case PERSISTENT_VERSION_CHANGED: return BuildSummaryChanged;
      case NOT_FINISHED: return PreviousBuildNotFinished;
      default: throw new IllegalArgumentException(inc.toString());
      }
    }
  }
  
  public void startedBuilder(Builder<?, ?> b, BuildUnit<?> oldUnit, Set<BuildReason> reasons);
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void skippedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  
  public <O extends Output> void canceledBuilderFailure(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void canceledBuilderException(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, Throwable t);
  public <O extends Output> void canceledBuilderInterrupt(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit);
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e);
  
  public void inconsistentRequirement(Requirement req);
  
  public void messageFromBuilder(String message, boolean isError, Builder<?, ?> from);
  public void exceptionFromBuilder(Throwable t, Builder<?, ?> from);
  
  public void messageFromSystem(String message, boolean isError);
}
