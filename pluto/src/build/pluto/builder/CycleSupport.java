package build.pluto.builder;

import java.util.Set;

import build.pluto.BuildUnit;

public interface CycleSupport {
  
  public String cycleDescription();

  public boolean canBuildCycle();

  public Set<BuildUnit<?>> buildCycle(BuildUnitProvider manager) throws Throwable;

}
