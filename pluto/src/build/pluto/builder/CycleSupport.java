package build.pluto.builder;

import java.util.Set;

import build.pluto.BuildUnit;

/**
 * A CycleSupport is created for a build cycle and used to build it. A
 * CycleSupport may reject to build the cycle.
 * 
 * @author moritzlichter
 *
 */
public interface CycleSupport {
  
  /**
   * @return a description of the cycle to compile.
   */
  public String cycleDescription();

  /**
   * Determines whether the cycle can compiled.
   * {@code #buildCycle(BuildUnitProvider)} is only called if this method
   * returns true.
   * 
   * @return whether the cycle support is able to build the cycle (for which the
   *         cycle support was created).
   */
  public boolean canBuildCycle();

  /**
   * Build the cycle. This implements the strategy used to build the cycle. The
   * given manager is the BuildManager which detected the cycle. It has to be
   * used by the cycle support to delegate requireBuild calls which are not part
   * of the cycle.
   * 
   * @param manager
   *          an instance of a BuildUnitProvider, which can be used to
   * @return a set of build units. There is one unit for one request in the
   *         cycle exactly.
   * @throws Throwable
   */
  public Set<BuildUnit<?>> buildCycle(BuildUnitProvider manager) throws Throwable;
  
}
