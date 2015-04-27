package build.pluto.builder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.util.AbsoluteComparedFile;
import build.pluto.util.UniteCollections;

public class RequireStack extends CycleDetectionStack<BuildRequest<?, ?, ?, ?>, Boolean> {

  private Set<BuildRequest<?, ?, ?, ?>> knownInconsistentUnits;
  private Set<BuildRequest<?, ?, ?, ?>> consistentUnits;
  
  public RequireStack() {
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
  }

  public void beginRebuild(BuildRequest<?, ?, ?, ?> dep) {
    
    Log.log.log("Rebuild " + dep.createBuilder().description(), Log.DETAIL);
    
    this.knownInconsistentUnits.add(dep);
    // TODO: Need to forget the scc where dep is in, because the graph structure
    // may change?
   // for (Path assumed : this.requireStack) {
      // This could be too strict
      // this.knownInconsistentUnits.add(assumed);
     // this.consistentUnits.remove(assumed);
    //}
  }

  private String printCyclicConsistentAssumtion(BuildRequest<?, ?, ?, ?> dep) {
    UniteCollections<BuildRequest<?, ?, ?, ?>, List<BuildRequest<?, ?, ?, ?>>>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return "";
    }
    String s = "";
    for (BuildRequest<?, ?, ?, ?> p : this.sccs.getSetMembers(key)) {
      s += p.createBuilder().description() + ", ";
    }
    return s;
  }

  public void finishRebuild(BuildRequest<?, ?, ?, ?> dep) {
    this.consistentUnits.add(dep);
    Log.log.log("MARK INCONSISTENT " + dep.createBuilder().description(), Log.CORE);
    this.knownInconsistentUnits.remove(dep);
    // Allowed to do that in any case?
    // this.assumedCyclicConsistency.remove(dep);
  }

  public boolean isKnownInconsistent(File dep) {
    AbsoluteComparedFile aDep = AbsoluteComparedFile.absolute(dep);
    return knownInconsistentUnits.contains(aDep);// ||
                                                 // this.isAssumtionKnownInconsistent(aDep);
  }

  public boolean isAssumtionKnownInconsistent(BuildRequest<?, ?, ?, ?> dep) {
    UniteCollections<BuildRequest<?, ?, ?, ?>, List<BuildRequest<?, ?, ?, ?>>>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return false;
    }
    for (BuildRequest<?, ?, ?, ?> p : this.sccs.getSetMembers(key)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public BuildCycle createCycleFor(BuildRequest<?, ?, ?, ?> dep) {
    List<BuildRequest<?, ?, ?, ?>> deps = sccs.getSetMembers(sccs.getSet(dep));
    return new BuildCycle(dep, deps);
  }

  public boolean isConsistent(BuildRequest<?, ?, ?, ?> dep) {
    return this.consistentUnits.contains(dep);
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
    Log.log.log("MARK CONSISTENT " + dep.createBuilder().description(), Log.CORE);
    this.consistentUnits.add(dep);
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
