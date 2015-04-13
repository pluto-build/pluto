package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.util.UniteSets;
import org.sugarj.common.FileCommands;

public class ExecutingStack {

  private List<BuildUnit<?>> requireCallStack = new ArrayList<>();
  
  private UniteSets<BuildUnit<?>> sccs = new UniteSets<>();
  
  protected void push(BuildUnit<?> unit) throws IOException{
    int index = this.requireCallStack.indexOf(unit);
    if (index != -1) {
      UniteSets<BuildUnit<?>>.Key scc = sccs.getOrCreateSet(unit);     
      // All units on the cyclic stack area are in one scc, this may merge existing ones
      for (int i = index; scc != null && i < requireCallStack.size(); i ++) {
        scc = sccs.uniteOrAdd(scc, requireCallStack.get(i));
      }
      
      // Get all elements of the scc
      List<BuildRequirement<?>>  cycleComponents = new ArrayList<>();
      for (BuildUnit<?> p : sccs.getSetMembers(scc)) {
        cycleComponents.add(requirementForEntry(p));
      }
      
      BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + FileCommands.tryGetRelativePath(unit.getPersistentPath()), unit, cycleComponents);
     throw ex;
    }
    this.requireCallStack.add(unit);
  }
  
  public int getNumContains(BuildUnit<?> path) {
    int num = 0;
    for (BuildUnit<?> p  : requireCallStack) {
      if (p == path) {
        num ++;
      }
    }
    return num;
  }
  
  private <Out extends Serializable> BuildRequirement<Out> requirementForEntry(BuildUnit<Out> unit) throws IOException{
    return new BuildRequirement<>(unit, unit.getGeneratedBy());
  }

  protected void pop(BuildUnit<?> required) {
    BuildUnit<?> poppedEntry = requireCallStack.remove(requireCallStack.size()-1);
    assert poppedEntry == required : "Got the wrong build stack entry from the requires stack";
  }

}
