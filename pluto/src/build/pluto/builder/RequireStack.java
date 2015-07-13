package build.pluto.builder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.util.AbsoluteComparedFile;
import build.pluto.util.IReporting.BuildReason;

public class RequireStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Boolean> {

  private Map<BuildRequest<?, ?, ?, ?>, Set<BuildReason>> knownInconsistentUnits;
  private Set<BuildRequest<?, ?, ?, ?>> knownConsistentUnits;
  private Set<BuildRequest<?, ?, ?, ?>> assumedUnits;
  
  public RequireStack() {
    this.knownConsistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashMap<>();
    this.assumedUnits = new HashSet<>();
  }

  public void beginRebuild(BuildRequest<?, ?, ?, ?> dep, Set<BuildReason> reason) {
    Log.log.log("Rebuild " + dep.createBuilder().description(), Log.DETAIL);
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
    Log.log.beginTask("Require " + dep.createBuilder().description(), Log.DETAIL);
    return super.push(dep);
  }

  @Override
  public void pop(BuildRequest<?, ?, ?, ?> dep) {
    super.pop(dep);
    Log.log.endTask();
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
    Log.log.log("Already required " + call.createBuilder().description(), Log.DETAIL);
    this.callStack.add(call);
    return true;
  }

  @Override
  protected Boolean noCycleResult() {
    return false;
  }

}
