package build.pluto.builder;

import java.util.Objects;
import java.util.function.Function;

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
    Objects.requireNonNull(cycle);
    Objects.requireNonNull(cycleCause);
    assert cycle.getCycleComponents().contains(cycleCause) : "Cause " + cycleCause.createBuilder().description() + " not in cycle {" + cycle.getCycleComponents().stream().map(((Function<BuildRequest, Builder>) BuildRequest::createBuilder).andThen(Builder::description)).reduce((String s1, String s2) -> s2 + " , " + s2).get() + "}";
    this.cycleCause = cycleCause;
    this.cycle = cycle;
  }

  public BuildCycle getCycle() {
    return cycle;
  }

  public boolean isFirstInvokedOn(BuildRequest<?, ?, ?, ?> unit) {
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
