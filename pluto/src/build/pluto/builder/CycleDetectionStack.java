package build.pluto.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import build.pluto.util.UniteCollections;
import build.pluto.util.UniteCollections.Supplier;

public abstract class CycleDetectionStack<C, P> {

 protected List<C> callStack = new ArrayList<>();
  
  protected UniteCollections<C, List<C>> sccs = new UniteCollections<>(new Supplier<List<C>>() {
    public ArrayList<C> get() {
      return new ArrayList<>();
    }
  });
  
  protected P push(C unit) {
    // Check whether unit is already on the stack
    int index = this.callStack.indexOf(unit);
    if (index != -1) {
      unit = callStack.get(index);
      // Then unite the sccs of all units from the top of the stack until
      // the already existing occurence of unit
      UniteCollections<C, List<C>>.Key scc = sccs.getOrCreate(unit);
      for (int i = index+1; i < callStack.size(); i++) {
        scc = sccs.uniteOrAdd(scc, callStack.get(i));
      }
      // Subclasses decide what to return
      return cycleResult(unit, sccs.getSetMembers(scc));
    } else {
      sccs.getOrCreate(unit);
    }
    // No cycle, put unit on the stack
    this.callStack.add(unit);
    return noCycleResult();
  }
  
  protected abstract P cycleResult(C call, List<C> scc);
  protected abstract P noCycleResult();
  
  public int getNumContains(C elem) {
    int num = 0;
    for (C p  : callStack) {
      if (p.equals(elem)) {
        num ++;
      }
    }
    return num;
  }
  
  protected void pop(C required) {
    C poppedEntry = callStack.remove(callStack.size()-1);
    assert poppedEntry.equals(required) : "Got the wrong build stack entry from the stack";
  }
  
  protected C topMostEntry(Collection<C> reqs) {
    for (C r : this.callStack) {
      if (reqs.contains(r)) {
        return r;
      }
    }
    return null;
  }

}
