package build.pluto.builder;

import java.util.Objects;
import java.util.Set;

import build.pluto.BuildUnit;

public class BuildCycleException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = -8981404220171314788L;

  public static enum CycleState {
    RESOLVED, NOT_RESOLVED, UNHANDLED
  }


  /**
   * The {@link BuildUnit} that caused the cycle.
   */
  private final BuildRequest<?, ?, ?, ?> cycleCause;
  /**
   * The set 
   */
  private final BuildCycle cycle;
  private CycleState cycleState = CycleState.UNHANDLED;
  private BuildCycleResult cycleResult = null;

  public BuildCycleException(String message, BuildRequest<?, ?, ?, ?> cycleCause, BuildCycle cycle) {
    super(message);
    Objects.requireNonNull(cycleCause);
    this.cycleCause = cycleCause;
    this.cycle = cycle;
  }

  public Set<BuildRequest<?, ?, ?, ?>> getCycleComponents() {
    return cycle.getCycleComponents();
  }

  public boolean isUnitFirstInvokedOn(BuildRequest<?, ?, ?, ?> unit) {
    return cycleCause.equals(unit);
  }

  public void setCycleState(CycleState cycleState) {
    this.cycleState = cycleState;
  }

  public CycleState getCycleState() {
    return cycleState;
  }

  public BuildRequest<?, ?, ?, ?> getCycleCause() {
    return cycleCause;
  }
  
  public void setCycleResult(BuildCycleResult cycleResult) {
    this.cycleResult = cycleResult;
  }
  
  public BuildCycleResult getCycleResult() {
    return cycleResult;
  }

}
