package build.pluto.builder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import build.pluto.util.AbsoluteComparedFile;
import build.pluto.util.IReporting;
import build.pluto.util.IReporting.BuildReason;

public class RequireStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Boolean> {

  private final IReporting report;
  private final Map<BuildRequest<?, ?, ?, ?>, Set<BuildReason>> knownInconsistentUnits;
  private final Set<BuildRequest<?, ?, ?, ?>> knownConsistentUnits;
  private final Set<BuildRequest<?, ?, ?, ?>> assumedUnits;
  
  public RequireStack(IReporting report) {
    this.report = report;
    this.knownConsistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashMap<>();
    this.assumedUnits = new HashSet<>();
  }

  public void beginRebuild(BuildRequest<?, ?, ?, ?> dep, Set<BuildReason> reason) {
    this.knownInconsistentUnits.put(dep, reason);
    // TODO: Need to forget the scc where dep is in, because the graph structure
    // may change?
  }

  public void finishRebuild(BuildRequest<?, ?, ?, ?> dep) {
    this.knownConsistentUnits.add(dep);
    this.knownInconsistentUnits.remove(dep);
  }

  public Set<BuildReason> isKnownInconsistent(File dep) {
    AbsoluteComparedFile aDep = AbsoluteComparedFile.absolute(dep);
    return knownInconsistentUnits.get(aDep);
  }

  public boolean existsInconsistentCyclicRequest(BuildRequest<?, ?, ?, ?> dep) {
    return this.sccs.getSetMembers(dep).stream().anyMatch(this.knownInconsistentUnits::containsKey);
  }

  public boolean areAllOtherCyclicRequestsAssumed(BuildRequest<?, ?, ?, ?> dep) {
    return this.sccs.getSetMembers(dep).stream().filter((BuildRequest<?, ?, ?, ?> req) -> req != dep).allMatch(this.assumedUnits::contains);
  }

  public BuildCycle createCycleFor(BuildRequest<?, ?, ?, ?> dep) {
    List<BuildRequest<?, ?, ?, ?>> deps = sccs.getSetMembers(sccs.getSet(dep));
    return new BuildCycle(dep, deps);
  }

  public boolean isConsistent(BuildRequest<?, ?, ?, ?> dep) {
    return this.knownConsistentUnits.contains(dep);
  }
  
  @Override
  protected Boolean push(BuildRequest<?, ?, ?, ?> dep) {
    report.buildRequirement(dep);
    return super.push(dep);
  }

  @Override
  public void pop(BuildRequest<?, ?, ?, ?> dep) {
    report.finishedBuildRequirement(dep);
    super.pop(dep);
  }

  public void markConsistent(BuildRequest<?, ?, ?, ?> dep) {
    this.knownConsistentUnits.add(dep);
  }

  public void markAllConsistent(BuildRequest<?, ?, ?, ?> dep) {
    this.sccs.getSetMembers(dep).forEach(this.knownConsistentUnits::add);
  }

  public void markAssumed(BuildRequest<?, ?, ?, ?> dep) {
    this.assumedUnits.add(dep);
  }

  @Override
  protected Boolean cycleResult(BuildRequest<?, ?, ?, ?> call, List<BuildRequest<?, ?, ?, ?>> scc) {
    report.messageFromSystem("Already required " + call, false, 7);
    this.callStack.add(call);
    return true;
  }

  @Override
  protected Boolean noCycleResult() {
    return false;
  }

}
