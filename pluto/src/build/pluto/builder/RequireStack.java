package build.pluto.builder;

import java.util.HashSet;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

import build.pluto.util.UniteSets;

public class RequireStack extends CycleDetectionStack<Path, Boolean> {

  private Set<Path> knownInconsistentUnits;
  private Set<Path> consistentUnits;

  public RequireStack() {
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
  }

  public void beginRebuild(Path dep) {
    Log.log.log("Rebuild " + FileCommands.tryGetRelativePath(dep), Log.DETAIL);
    Log.log.log("Assumptions: " + printCyclicConsistentAssumtion(dep), Log.DETAIL);
    
    this.knownInconsistentUnits.add(dep);
    // TODO: Need to forget the scc where dep is in, because the graph structure
    // may change?
   // for (Path assumed : this.requireStack) {
      // This could be too strict
      // this.knownInconsistentUnits.add(assumed);
     // this.consistentUnits.remove(assumed);
    //}
  }

  private String printCyclicConsistentAssumtion(Path dep) {
    UniteSets<Path>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return "";
    }
    String s = "";
    for (Path p : this.sccs.getSetMembers(key)) {
      s += FileCommands.tryGetRelativePath(p) + ", ";
    }
    return s;
  }

  public void finishRebuild(Path dep) {
    this.consistentUnits.add(dep);
    this.knownInconsistentUnits.remove(dep);
    // Allowed to do that in any case?
    // this.assumedCyclicConsistency.remove(dep);
  }

  public boolean isKnownInconsistent(Path dep) {
    return knownInconsistentUnits.contains(dep) || this.isAssumtionKnownInconsistent(dep);
  }

  private boolean isAssumtionKnownInconsistent(Path dep) {
    UniteSets<Path>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return false;
    }
    for (Path p : this.sccs.getSetMembers(key)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public boolean isConsistent(Path dep) {
    return this.consistentUnits.contains(dep);
  }

  @Override
  protected Boolean push(Path dep) {
    Log.log.beginTask("Require " + FileCommands.tryGetRelativePath(dep), Log.DETAIL);
    return super.push(dep);
  }

  @Override
  public void pop(Path dep) {
    super.pop(dep);
    Log.log.endTask();
  }

  public void markConsistent(Path dep) {
    this.consistentUnits.add(dep);
  }

  @Override
  protected Boolean cycleResult(Path call, Set<Path> scc) {
    Log.log.log("Already required " + FileCommands.tryGetRelativePath(call), Log.DETAIL);
    this.callStack.add(call);
    return true;
  }

  @Override
  protected Boolean noCycleResult() {
    return false;
  }

}
