package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;

public class ExecutingStack {

  private List<BuildStackEntry<?>> requireCallStack = new ArrayList<>();

  protected <Out extends Serializable>  BuildStackEntry<Out> push(BuildUnit<Out>unit) throws IOException{
    BuildStackEntry<Out> entry = new BuildStackEntry<Out>(unit);

    int index = this.requireCallStack.indexOf(entry);
    if (index != -1) {
      List<BuildRequirement<?>>  cycleComponents = new ArrayList<>();
      for (; index < requireCallStack.size(); index ++) {
        cycleComponents.add(requirementForEntry(requireCallStack.get(index)));
      }
      BuildCycleException ex = new BuildCycleException("Build contains a dependency cycle on " + unit.getPersistentPath(), entry, cycleComponents);
     throw ex;
    }
    this.requireCallStack.add(entry);
    return entry;
  }
  
  public int getNumContains(BuildUnit<?> unit) {
    int num = 0;
    for (BuildStackEntry<?> e : requireCallStack) {
      if (e.getUnit() == unit) {
        num ++;
      }
    }
    return num;
  }
  
  private <Out extends Serializable> BuildRequirement<Out> requirementForEntry(BuildStackEntry<Out> entry) throws IOException{
    BuildUnit<Out> unit = entry.getUnit();
    return new BuildRequirement<>(unit, unit.getGeneratedBy());
  }

  protected BuildStackEntry<?> pop() {
    BuildStackEntry<?> poppedEntry = requireCallStack.remove(requireCallStack.size()-1);
    return poppedEntry;
  }

}
