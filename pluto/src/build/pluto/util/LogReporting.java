package build.pluto.util;

import java.util.BitSet;
import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public class LogReporting implements IReporting {

  private Log log = Log.log;
  
  int index = 0;
  private BitSet stack = new BitSet();
  
  @Override
  public void startedBuilder(Builder<?, ?> b, BuildUnit<?> oldUnit, Set<BuildReason> reasons) {
    String desc = b.description();
    stack.set(index, desc != null);
    index++;
    if (desc != null)
      log.beginTask(desc, Log.CORE);
  }

  @Override
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    index--;
    if (stack.get(index))
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
  public void exceptionFromBuilder(Throwable t, Builder<?, ?> from) {
    log.logErr(t.getClass() + (t == null ? "" : (": " + t.getMessage())), Log.CORE);
  }

  @Override
  public void messageFromSystem(String message, boolean isError) {
    log.log(message, Log.CORE);
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
  }

  @Override
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e) {
    log.endTask(e.getMessage());
  }

}
