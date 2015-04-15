package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.util.UniteSets;

public abstract class CycleDetectionStack<C, P> {

private List<C> requireCallStack = new ArrayList<>();
  
  protected UniteSets<C> sccs = new UniteSets<>();
  
  protected P push(C unit) {
    /**
     *  for (Path cycleDep : requireStack) { 
        if (cycleDep.equals(dep)) {
          break;
        }
        scc = this.sccAssumedConsistent.uniteOrAdd(scc, cycleDep);
      }
     */
    int index = this.requireCallStack.indexOf(unit);
    if (index != -1) {
      UniteSets<C>.Key scc = sccs.getOrCreateSet(unit);     
      // All units on the cyclic stack area are in one scc, this may merge existing ones
      for (int i = index; scc != null && i < requireCallStack.size(); i ++) {
        scc = sccs.uniteOrAdd(scc, requireCallStack.get(i));
      }
      // Get all elements of the scc
      return cycleResult(unit, sccs.getSetMembers(scc));
    }
    this.requireCallStack.add(unit);
    return noCycleResult();
  }
  
  protected abstract P cycleResult(C call, Set<C> scc);
  protected abstract P noCycleResult();
  
  public int getNumContains(BuildUnit<?> path) {
    int num = 0;
    for (C p  : requireCallStack) {
      if (p.equals(path)) {
        num ++;
      }
    }
    return num;
  }
  
  protected void pop(C required) {
    C poppedEntry = requireCallStack.remove(requireCallStack.size()-1);
    assert poppedEntry == required : "Got the wrong build stack entry from the stack";
  }
  
}
