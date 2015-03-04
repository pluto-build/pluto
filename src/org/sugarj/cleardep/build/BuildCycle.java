package org.sugarj.cleardep.build;

import java.util.List;

import org.sugarj.cleardep.dependency.BuildRequirement;

public class BuildCycle {
  
  public static class Result {
    
  }
  
  private List<BuildRequirement<?>> cycleComponents;

  public BuildCycle(List<BuildRequirement<?>> cycleComponents) {
    super();
    this.cycleComponents = cycleComponents;
  }
  
  public List<BuildRequirement<?>> getCycleComponents() {
    return cycleComponents;
  }

}
