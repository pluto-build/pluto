package build.pluto.util;

import java.util.LinkedList;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public class TraceReport implements IReporting {

  private static class FrameData {
    public int localDuration;
    public long initialStart;
    public long lastStart;
    public FrameData(long initialStart) {
      this.localDuration = 0;
      this.initialStart = initialStart;
      this.lastStart = initialStart;
    }
  }
  
  private final IReporting report;
  
  private LinkedList<FrameData> durationStack = new LinkedList<>();
  
  public TraceReport(IReporting report) {
    this.report = report;
    durationStack.add(new FrameData(-1)); // root frame
  }
  
  @Override
  public <O extends Output> void buildRequirement(BuildRequest<?, O, ?, ?> req) {
    report.buildRequirement(req);
  }

  @Override
  public <O extends Output> void finishedBuildRequirement(BuildRequest<?, O, ?, ?> req) {
    report.finishedBuildRequirement(req);
  }

  @Override
  public <O extends Output> void startedBuilder(BuildRequest<?, O, ?, ?> req, Builder<?, ?> b, BuildUnit<O> oldUnit, Set<BuildReason> reasons) {
    long endTime = System.currentTimeMillis();
    report.startedBuilder(req, b, oldUnit, reasons);
    
    FrameData frame = durationStack.peek();
    if (frame.lastStart > 0)
      frame.localDuration += endTime - frame.lastStart;

    durationStack.push(new FrameData(endTime));
  }

  @Override
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    report.finishedBuilder(req, unit);
    long endTime = System.currentTimeMillis();
    
    FrameData frame = durationStack.pop();
    int localDuration = frame.localDuration + (int) (endTime - frame.lastStart);
    int totalDuration = (int) (endTime - frame.initialStart);
    unit.setTrace(new TraceData(localDuration, totalDuration));
    
    durationStack.peek().lastStart = endTime;
  }

  @Override
  public <O extends Output> void skippedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    report.skippedBuilder(req, unit);
  }

  @Override
  public <O extends Output> void canceledBuilderFailure(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    report.canceledBuilderFailure(req, unit);
  }

  @Override
  public <O extends Output> void canceledBuilderException(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, Throwable t) {
    report.canceledBuilderException(req, unit, t);
  }

  @Override
  public <O extends Output> void canceledBuilderInterrupt(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    report.canceledBuilderInterrupt(req, unit);
  }

  @Override
  public <O extends Output> void canceledBuilderRequiredBuilderFailed(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, RequiredBuilderFailed e) {
    report.canceledBuilderRequiredBuilderFailed(req, unit, e);
  }

  @Override
  public void startBuildCycle(BuildCycle cycle, CycleSupport cycleSupport) {
    report.startBuildCycle(cycle, cycleSupport);
  }

  @Override
  public void finishedBuildCycle(BuildCycle cycle, CycleSupport cycleSupport, Set<BuildUnit<?>> units) {
    report.finishedBuildCycle(cycle, cycleSupport, units);
  }

  @Override
  public void cancelledBuildCycleException(BuildCycle cycle, CycleSupport cycleSupport, Throwable t) {
    report.cancelledBuildCycleException(cycle, cycleSupport, t);
  }

  @Override
  public void inconsistentRequirement(Requirement req) {
    report.inconsistentRequirement(req);
  }

  @Override
  public void messageFromBuilder(String message, boolean isError, Builder<?, ?> from) {
    report.messageFromBuilder(message, isError, from);
  }

  @Override
  public void messageFromSystem(String message, boolean isError, int verbosity) {
    report.messageFromSystem(message, isError, verbosity);
  }

}
