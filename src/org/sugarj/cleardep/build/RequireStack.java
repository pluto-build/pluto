package org.sugarj.cleardep.build;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.util.UniteSets;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class RequireStack {

  private Deque<Path> requireStack;
  private Set<Path> knownInconsistentUnits;
  private Set<Path> consistentUnits;
  private UniteSets<Path> sccAssumedConsistent;

  private final boolean LOG_REQUIRE = true;

  public RequireStack() {
    this.sccAssumedConsistent = new UniteSets<>();
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
    this.requireStack = new LinkedList<>();
  }

  public void beginRebuild(Path dep) {
    if (LOG_REQUIRE) {
      Log.log.log("Rebuild " + FileCommands.tryGetRelativePath(dep), Log.CORE);
      Log.log.log("Assumptions: " + printCyclicConsistentAssumtion(dep), Log.CORE);
    }
    this.knownInconsistentUnits.add(dep);
    // TODO: Need to forget the scc where dep is in, because the graph structure may change?
    for (Path assumed : this.requireStack) {
      // This could be too strict
      // this.knownInconsistentUnits.add(assumed);
      this.consistentUnits.remove(assumed);
    }
  }
  
  private String printCyclicConsistentAssumtion(Path dep) {
    UniteSets<Path>.Key key = this.sccAssumedConsistent.getSet(dep);
    if (key == null) {
      return "";
    }
    String s = "";
    for (Path p : this.sccAssumedConsistent.getSetMembers(key)) {
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
    UniteSets<Path>.Key key = this.sccAssumedConsistent.getSet(dep);
    if (key == null) {
      return false;
    }
    for (Path p : this.sccAssumedConsistent.getSetMembers(key)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public boolean isConsistent(Path dep) {
    return this.consistentUnits.contains(dep);
  }

  public boolean isAlreadyRequired(Path dep) {
    if (this.requireStack.contains(dep)) {
      if (LOG_REQUIRE)
        Log.log.log("Already required " + FileCommands.tryGetRelativePath(dep), Log.CORE);
      
      // Union the sccs which are connected by a cycle
      // In the cycle are all units from the top of the stack until the occurence of
      // dep is found
      UniteSets<Path>.Key scc = this.sccAssumedConsistent.getOrCreateSet(dep);
      for (Path cycleDep : requireStack) { 
        if (cycleDep.equals(dep)) {
          break;
        }
        scc = this.sccAssumedConsistent.uniteOrAdd(scc, cycleDep);
      }
      return true;
    }
    return false;
  }

  public void beginRequire(Path dep) {
    this.requireStack.push(dep);
    if (LOG_REQUIRE)
      Log.log.beginTask("Require " + FileCommands.tryGetRelativePath(dep), Log.CORE);
  }

  public void finishRequire(Path dep) {
    this.requireStack.pop();
    if (LOG_REQUIRE)
      Log.log.endTask();
  }

  public void markConsistent(Path dep) {
    this.consistentUnits.add(dep);
  }

}
