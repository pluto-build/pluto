package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.common.path.Path;

public class BuildCycleException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = -8981404220171314788L;

  public static enum CycleState {
    RESOLVED, NOT_RESOLVED, UNHANDLED
  }

  /**
   * The list of the stack entries which form a dependency cycle in order of the
   * stack.
   */
  private final BuildStackEntry<?> cycleCause;
  private final List<BuildRequirement<?>> cycleComponents;
  private CycleState cycleState = CycleState.UNHANDLED;
  private BuildCycle.Result cycleResult = null;

  public BuildCycleException(String message, BuildStackEntry<?> cycleCause, List<BuildRequirement<?>> cycleComponents) {
    super(message);
    Objects.requireNonNull(cycleCause);
    this.cycleCause = cycleCause;
    this.cycleComponents = cycleComponents;
  }

  public List<BuildRequirement<?>> getCycleComponents() {
    return cycleComponents;
  }

  public boolean isUnitFirstInvokedOn(Path path, BuilderFactory<?, ?, ?> factory) {
    return cycleCause.getUnit().getPersistentPath().equals(path) && cycleCause.getUnit().getGeneratedBy().factory.equals(factory);
  }

  public void setCycleState(CycleState cycleState) {
    this.cycleState = cycleState;
  }

  public CycleState getCycleState() {
    return cycleState;
  }

  public BuildStackEntry<?> getCycleCause() {
    return cycleCause;
  }
  
  public void setCycleResult(BuildCycle.Result cycleResult) {
    this.cycleResult = cycleResult;
  }
  
  public BuildCycle.Result getCycleResult() {
    return cycleResult;
  }

}
