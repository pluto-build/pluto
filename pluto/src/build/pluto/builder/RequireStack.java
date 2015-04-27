package build.pluto.builder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.util.AbsoluteComparedFile;
import build.pluto.util.UniteCollections;

public class RequireStack extends CycleDetectionStack<AbsoluteComparedFile, Boolean> {

  private Set<AbsoluteComparedFile> knownInconsistentUnits;
  private Set<AbsoluteComparedFile> consistentUnits;
  
  public RequireStack() {
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
  }

  public void beginRebuild(File dep) {

    AbsoluteComparedFile aDep = AbsoluteComparedFile.absolute(dep);
    
    Log.log.log("Rebuild " + dep, Log.DETAIL);
    Log.log.log("Assumptions: " + printCyclicConsistentAssumtion(aDep), Log.DETAIL);
    
    this.knownInconsistentUnits.add(aDep);
    // TODO: Need to forget the scc where dep is in, because the graph structure
    // may change?
   // for (Path assumed : this.requireStack) {
      // This could be too strict
      // this.knownInconsistentUnits.add(assumed);
     // this.consistentUnits.remove(assumed);
    //}
  }

  private String printCyclicConsistentAssumtion(AbsoluteComparedFile dep) {
    UniteCollections<AbsoluteComparedFile, List<AbsoluteComparedFile>>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return "";
    }
    String s = "";
    for (AbsoluteComparedFile p : this.sccs.getSetMembers(key)) {
      s += p.getFile() + ", ";
    }
    return s;
  }

  public void finishRebuild(File dep) {
    this.consistentUnits.add(AbsoluteComparedFile.absolute(dep));
    this.knownInconsistentUnits.remove(AbsoluteComparedFile.absolute(dep));
    // Allowed to do that in any case?
    // this.assumedCyclicConsistency.remove(dep);
  }

  public boolean isKnownInconsistent(File dep) {
    AbsoluteComparedFile aDep = AbsoluteComparedFile.absolute(dep);
    return knownInconsistentUnits.contains(aDep);// ||
                                                 // this.isAssumtionKnownInconsistent(aDep);
  }

  public boolean isAssumtionKnownInconsistent(File dep) {
    return isAssumtionKnownInconsistent(AbsoluteComparedFile.absolute(dep));
  }

  public BuildCycle createCycleFor(File dep) {
    List<AbsoluteComparedFile> deps = sccs.getSetMembers(sccs.getSet(AbsoluteComparedFile.absolute(dep)));
    Function<AbsoluteComparedFile, BuildRequest<?, ?, ?, ?>> extractReq = (AbsoluteComparedFile f) -> {
      // try {
      return BuildUnit.readFromMemoryCache(BuildUnit.class, f.getFile()).getGeneratedBy();

    };
    List<BuildRequest<?, ?, ?, ?>> reqs = deps.stream().map(extractReq).collect(Collectors.toList());
    return new BuildCycle(extractReq.apply(AbsoluteComparedFile.absolute(dep)), reqs);
  }

  private boolean isAssumtionKnownInconsistent(AbsoluteComparedFile dep) {
    UniteCollections<AbsoluteComparedFile, List<AbsoluteComparedFile>>.Key key = this.sccs.getSet(dep);
    if (key == null) {
      return false;
    }
    for (AbsoluteComparedFile p : this.sccs.getSetMembers(key)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public boolean isConsistent(File dep) {
    return this.consistentUnits.contains(AbsoluteComparedFile.absolute(dep));
  }

  protected Boolean push(File dep) {
    return push(AbsoluteComparedFile.absolute(dep));
  }
  
  @Override
  protected Boolean push(AbsoluteComparedFile dep) {
    Log.log.beginTask("Require " + dep.getFile(), Log.DETAIL);
    return super.push(dep);
  }
  
  public void pop(File dep) {
    this.pop(AbsoluteComparedFile.absolute(dep));
  }

  @Override
  public void pop(AbsoluteComparedFile dep) {
    super.pop(dep);
    Log.log.endTask();
  }

  public void markConsistent(File dep) {
    this.consistentUnits.add(AbsoluteComparedFile.absolute(dep));
  }

  @Override
  protected Boolean cycleResult(AbsoluteComparedFile call, List<AbsoluteComparedFile> scc) {
    Log.log.log("Already required " + call.getFile(), Log.DETAIL);
    this.callStack.add(call);
    return true;
  }

  @Override
  protected Boolean noCycleResult() {
    return false;
  }

}
