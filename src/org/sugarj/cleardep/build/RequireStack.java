package org.sugarj.cleardep.build;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.common.path.Path;

public class RequireStack {

  private transient Deque<Path> requireStack;
  private transient Set<Path> knownInconsistentUnits;
  private transient Set<Path> consistentUnits;
  // private transient ConsistencyManager consistencyManager;
  private transient Map<Path, Set<Path>> assumedCyclicConsistency;
  
  public RequireStack() {
    this.assumedCyclicConsistency = new HashMap<>();
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
    this.requireStack = new LinkedList<>();
  }
  
  public void beginRebuild(Path dep) {
    this.knownInconsistentUnits.add(dep);
    Set<Path> cyclicAssumptions = this.assumedCyclicConsistency.get(dep);
      for (Path assumed : this.requireStack) {
        this.knownInconsistentUnits.add(assumed);
        
        this.consistentUnits.remove(assumed);
      }
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
 // if (cyclicAssumptions == null || cyclicAssumptions.isEmpty()) {
    this.consistentUnits.add(dep);
    this.assumedCyclicConsistency.remove(dep);
 // }
  this.knownInconsistentUnits.remove(dep);
  }
  
  public boolean isKnownInconsistent(Path dep) {

    return knownInconsistentUnits.contains(dep);
  }
  
  public boolean isConsistent(Path dep) {
    return this.consistentUnits.contains(dep);
  }
  
  public boolean isAssumtionKnownInconsistent(Path dep) {

    for (Path p : this.getCyclicConsistentAssumtion(dep)) {
      if (this.knownInconsistentUnits.contains(p)) {
        return true;
      }
    }
    return false;
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
  
  public void finishRequire(Path dep) {
    this.requireStack.pop();
  }
  
  public void handleRequiredFinished(Path dep, Path required) {
    Set<Path> depAssumptions = this.getCyclicConsistentAssumtion(required);
    this.getCyclicConsistentAssumtion(dep).addAll(depAssumptions);
  }
  
  public void markConsistent(Path dep) {
    this.consistentUnits.add(dep);
  }
  
}
