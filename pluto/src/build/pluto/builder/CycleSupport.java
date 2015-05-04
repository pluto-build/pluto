package build.pluto.builder;

import java.util.Set;

import build.pluto.BuildUnit;

public interface CycleSupport {
  
  public String getCycleDescription();

  public boolean canCompileCycle();

  public Set<BuildUnit<?>> compileCycle(BuildUnitProvider manager) throws Throwable;

}
