package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.print.attribute.standard.MediaSize.Other;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

public class ExecutingStack {

  private List<BuildUnit<?>> requireCallStack = new ArrayList<>();
  
  private Map<BuildUnit<?>, Set<BuildUnit<?>>> stronglyConnectedComponentsRepresentants = new HashMap<>();
  private Map<BuildUnit<?>, BuildUnit<?>> stronglyConnectedComponentsMember = new HashMap<>();
  
  

  protected void push(BuildUnit<?> unit) throws IOException{
    int index = this.requireCallStack.indexOf(unit);
    if (index != -1) {
      List<BuildRequirement<?>>  cycleComponents = new ArrayList<>();
      BuildUnit<?> componentRepresentant = unit;
      Set<BuildUnit<?>> scc = stronglyConnectedComponentsRepresentants.get(componentRepresentant);
      
      for (int i = index; scc != null && i < requireCallStack.size(); i ++) {
        BuildUnit<?> cyclePath = requireCallStack.get(i);
        BuildUnit<?> cyclePathSSCRep = stronglyConnectedComponentsMember.get(cyclePath);
        if (cyclePathSSCRep != null) {
          componentRepresentant = cyclePathSSCRep;
          scc = stronglyConnectedComponentsRepresentants.get(componentRepresentant);
        }
      }
      
      if (scc == null) {
        scc = new HashSet<>();
        scc.add(componentRepresentant);
        stronglyConnectedComponentsRepresentants.put(componentRepresentant, scc);
      }
      
      for (int i = index; i < requireCallStack.size(); i ++) {
        BuildUnit<?> cyclePath = requireCallStack.get(i);
        BuildUnit<?> cyclePathSSCRep = stronglyConnectedComponentsMember.get(cyclePath);
        if (cyclePathSSCRep != componentRepresentant) {
          if (cyclePathSSCRep != null) {
            for (BuildUnit<?> otherSCCPart : stronglyConnectedComponentsRepresentants.get(cyclePathSSCRep)) {
              stronglyConnectedComponentsMember.put(otherSCCPart, componentRepresentant);
              scc.add(otherSCCPart);
            }
            stronglyConnectedComponentsRepresentants.remove(cyclePathSSCRep);
          } else {
            scc.add(cyclePath);
            stronglyConnectedComponentsMember.put(cyclePath, componentRepresentant);
          }
        }
      }
      for (BuildUnit<?> p : scc) {
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
