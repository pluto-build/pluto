package org.sugarj.cleardep.build;

public interface CycleSupport {
  
  public String getCycleDescription(BuildCycle cycle);
  public boolean canCompileCycle(BuildCycle cycle);
  public BuildCycle.Result compileCycle(BuildCycle cycle) throws Throwable;

}
