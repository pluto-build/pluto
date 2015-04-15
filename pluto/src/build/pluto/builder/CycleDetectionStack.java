package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.util.UniteSets;

public abstract class CycleDetectionStack<C, P> {

private List<C> callStack = new ArrayList<>();
  
  protected UniteSets<C> sccs = new UniteSets<>();
  
  protected P push(C unit) {
    // Check whether unit is already on the stack
    int index = this.callStack.indexOf(unit);
    if (index != -1) {
      // Then unite the sccs of all units from the top of the stack until
      // the already existing occurence of unit
      UniteSets<C>.Key scc = sccs.getOrCreateSet(unit);   
      scc = callStack.stream().skip(index).reduce(scc, sccs::uniteOrAdd, sccs::unite);
      // Subclasses decide what to return
      return cycleResult(unit, sccs.getSetMembers(scc));
    }
    // No cycle, put unit on the stack
    this.callStack.add(unit);
    return noCycleResult();
  }
  
  protected abstract P cycleResult(C call, Set<C> scc);
  protected abstract P noCycleResult();
  
  public int getNumContains(BuildUnit<?> path) {
    int num = 0;
    for (C p  : callStack) {
      if (p.equals(path)) {
        num ++;
      }
    }
    return num;
  }
  
  protected void pop(C required) {
    C poppedEntry = callStack.remove(callStack.size()-1);
    assert poppedEntry == required : "Got the wrong build stack entry from the stack";
  }
  
}
