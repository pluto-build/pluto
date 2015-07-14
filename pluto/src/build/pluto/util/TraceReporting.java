package build.pluto.util;

import java.util.LinkedList;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;

public class TraceReporting implements IReporting {

  private static class FrameData {
    public final TraceData oldData;
    public int localDuration;
    public final long initialStart;
    public long lastStart;
    public FrameData(TraceData oldData, long initialStart) {
      this.oldData = oldData;
      this.localDuration = 0;
      this.initialStart = initialStart;
      this.lastStart = initialStart;
    }
  }
  
  private final IReporting report;
  
  private LinkedList<FrameData> stack = new LinkedList<>();
  
  public TraceReporting(IReporting report) {
    this.report = report;
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
    
    FrameData frame = stack.peek();
    if (frame != null)
      frame.localDuration += endTime - frame.lastStart;
    
    TraceData oldData = oldUnit == null ? null : oldUnit.getTrace();
    stack.push(new FrameData(oldData, endTime));
  }

  @Override
  public <O extends Output> void finishedBuilder(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit) {
    report.finishedBuilder(req, unit);
    long endTime = System.currentTimeMillis();
    
    FrameData frame = stack.pop();
    int localDuration = frame.localDuration + (int) (endTime - frame.lastStart);
    int totalDuration = (int) (endTime - frame.initialStart);
    
    TraceData data = makeTraceData(frame, localDuration, totalDuration);
    unit.setTrace(data);
    
    if (!stack.isEmpty())
      stack.peek().lastStart = endTime;
  }

  private double updateAvg(double oldAvg, double n, double xn) {
    double norm = n <= 1 ? 0 : (n-1)/n;
    return norm*oldAvg + xn / n;
  }
  
  private double updateVar(double oldVar, double oldAvg, double n, double xn) {
    if (n <= 1)
      return 0;
    double diff = xn - oldAvg;
    return (n-2)*oldVar/(n-1) + diff * diff / n;
  }
  
  private TraceData makeTraceData(FrameData frame, int localDuration, int totalDuration) {
    int oldBuilds;
    int oldLocal;
    int oldTotal;
    double oldLocalVar;
    double oldTotalVar;
    if (frame.oldData == null) {
      oldBuilds = 0;
      oldLocal = 0;
      oldTotal = 0;
      oldLocalVar = 0;
      oldTotalVar = 0;
    }
    else {
      TraceData data = frame.oldData;
      oldBuilds = data.builds;
      oldLocal = data.localDuration;
      oldLocalVar = data.localDurationVariance;
      oldTotal = data.totalDuration;
      oldTotalVar = data.totalDurationVariance;
    }
    
    int builds = oldBuilds + 1;
    int localAvg = (int) updateAvg(oldLocal, builds, localDuration);
    int totalAvg = (int) updateAvg(oldTotal, builds, totalDuration);
    double localVar = updateVar(oldLocalVar, oldLocal, builds, localDuration);
    double totalVar = updateVar(oldTotalVar, oldTotal, builds, totalDuration);
    
    return new TraceData(builds, localAvg, localVar, totalAvg, totalVar);
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
  public <O extends Output> void canceledBuilderCycle(BuildRequest<?, O, ?, ?> req, BuildUnit<O> unit, BuildCycleException t) {
    report.canceledBuilderCycle(req, unit, t);
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
