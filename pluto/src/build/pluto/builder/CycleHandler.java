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
public abstract class CycleHandler {
  
  private final BuildCycle cycle;

  public CycleHandler(BuildCycle cycle) {
    super();
    this.cycle = cycle;
  }

  /**
   * @param cycle
   *          the cycle to get a description for
   * @return a description of the cycle to compile.
   */
  protected abstract String cycleDescription(BuildCycle cycle);

  public final String cycleDescription() {
    return cycleDescription(cycle);
  }

  /**
   * Determines whether the cycle can compiled.
   * {@code #buildCycle(BuildUnitProvider)} is only called if this method
   * returns true.
   * 
   * @param cycle
   *          to cycle to check
   * @return whether the cycle support is able to build the cycle (for which the
   *         cycle support was created).
   */
  protected abstract boolean canBuildCycle(BuildCycle cycle);

  protected final boolean canBuildCycle() {
    return canBuildCycle(cycle);
  }

  /**
   * Build the cycle. This implements the strategy used to build the cycle. The
   * given manager is the BuildManager which detected the cycle. It has to be
   * used by the cycle support to delegate requireBuild calls which are not part
   * of the cycle.
   * 
   * @param cycle
   *          the cycle to build
   * @param manager
   *          an instance of a BuildUnitProvider, which can be used to
   * @return a set of build units. There is one unit for one request in the
   *         cycle exactly.
   * @throws Throwable
   */
  protected abstract Set<BuildUnit<?>> buildCycle(BuildCycle cycle, BuildUnitProvider manager) throws Throwable;

  protected final Set<BuildUnit<?>> buildCycle(BuildUnitProvider manager) throws Throwable {
    return buildCycle(cycle, manager);
  }
  
}
