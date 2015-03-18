package org.sugarj.cleardep.build;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.path.Path;

public class RequireStack {

  private Deque<Path> requireStack;
  private Set<Path> knownInconsistentUnits;
  private Set<Path> consistentUnits;
  private Map<Path, Set<Path>> assumedCyclicConsistency;

  public RequireStack() {
    this.assumedCyclicConsistency = new HashMap<>();
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
    this.requireStack = new LinkedList<>();
  }

  public void beginRebuild(Path dep) {
    this.knownInconsistentUnits.add(dep);
//    for (Path assumed : this.requireStack) {
//      // This could be too strict
//      this.knownInconsistentUnits.add(assumed);
//      this.consistentUnits.remove(assumed);
//    }
  }

  private Set<Path> getCyclicConsistentAssumtion(Path dep) {
    Set<Path> cyclicAssumptions = assumedCyclicConsistency.get(dep);
    if (cyclicAssumptions == null) {
      cyclicAssumptions = new HashSet<Path>();
      assumedCyclicConsistency.put(dep, cyclicAssumptions);
    }
    return cyclicAssumptions;
  }

  public void finishRebuild(Path dep) {
    this.consistentUnits.add(dep);
    this.knownInconsistentUnits.remove(dep);
    // Allowed to do that in any case?
    this.assumedCyclicConsistency.remove(dep);
  }

  public boolean isKnownInconsistent(Path dep) {
    return knownInconsistentUnits.contains(dep) || this.isAssumtionKnownInconsistent(dep);
  }
  
  private boolean isAssumtionKnownInconsistent(Path dep) {
    for (Path p : this.getCyclicConsistentAssumtion(dep)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public boolean isConsistent(Path dep) {
    return this.consistentUnits.contains(dep);
  }

  public boolean isAlreadyRequired(Path source, Path dep) {
    if (this.requireStack.contains(dep)) {
      Set<Path> cyclicAssumptions = this.getCyclicConsistentAssumtion(dep);
      Set<Path> unitAssumptions = this.getCyclicConsistentAssumtion(source);
      unitAssumptions.addAll(cyclicAssumptions);
      unitAssumptions.add(dep);
      cyclicAssumptions.addAll(unitAssumptions);
      cyclicAssumptions.add(source);
      return true;
    }
    return false;
  }

  public void beginRequire(Path dep) {
    this.requireStack.push(dep);
  }

  public void finishRequire(Path source, Path dep) {
    if (source != null) {
      this.handleRequiredFinished(source, dep);
    }
    this.requireStack.pop();
  }

  private void handleRequiredFinished(Path dep, Path required) {
    Set<Path> depAssumptions = this.getCyclicConsistentAssumtion(required);
    this.getCyclicConsistentAssumtion(dep).addAll(depAssumptions);
  }

  public void markConsistent(Path dep) {
    this.consistentUnits.add(dep);
  }

}
