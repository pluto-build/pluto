package build.pluto.test.build;

import java.util.Set;

import junit.framework.AssertionFailedError;
import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleHandler;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;
import build.pluto.util.IReporting;

public class EnsureNoBuilderStartedReporting implements IReporting {

  @Override
  public <O extends Output> void buildRequirement(BuildRequest<?, O, ?, ?> req) {
  }

  @Override
  public <O extends Output> void finishedBuildRequirement(BuildRequest<?, O, ?, ?> req) {
  }

  @Override
  public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons) {
    throw new AssertionFailedError("Builder was started " + req);
  }

  @Override
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
  }

  @Override
  public <O extends Output> void skippedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
  }

  @Override
  public <O extends Output> void canceledBuilderFailure(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
  }

  @Override
  public <O extends Output> void canceledBuilderException(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, Throwable t) {
  }

  @Override
  public <O extends Output> void canceledBuilderCycle(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, BuildCycleException t) {
  }

  @Override
  public <O extends Output> void canceledBuilderInterrupt(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
  }

  @Override
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e) {
  }

  @Override
  public void startBuildCycle(BuildCycle cycle, CycleHandler cycleSupport) {
  }

  @Override
  public void finishedBuildCycle(BuildCycle cycle, CycleHandler cycleSupport, Set<BuildUnit<?>> units) {
  }

  @Override
  public void cancelledBuildCycleException(BuildCycle cycle, CycleHandler cycleSupport, Throwable t) {
  }

  @Override
  public void inconsistentRequirement(Requirement req) {
  }

  @Override
  public void messageFromBuilder(String message, boolean isError, Builder<?, ?> from) {
  }

  @Override
  public void messageFromSystem(String message, boolean isError, int verbosity) {
  }

}
