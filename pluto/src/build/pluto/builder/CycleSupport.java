package build.pluto.builder;

import java.util.Set;

import build.pluto.BuildUnit;

public interface CycleSupport {
  
  public String getCycleDescription(BuildCycle cycle);
  public boolean canCompileCycle(BuildCycle cycle);

  public Set<BuildUnit<?>> compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable;

}
