package build.pluto.builder;

public interface CycleSupport {
  
  public String getCycleDescription(BuildCycle cycle);
  public boolean canCompileCycle(BuildCycle cycle);
  public BuildCycleResult compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable;

}
