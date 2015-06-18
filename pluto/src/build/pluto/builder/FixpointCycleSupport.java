package build.pluto.builder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.sugarj.common.Log;

import build.pluto.BuildUnit;

/**
 * The {@link FixpointCycleSupport} resolved cycles by fixpoint compiling: the
 * requests in the cycle are compiled again and again until a consistent state
 * is reached. The involved builders need to be designed to support this
 * strategy of resolving a cycle, otherwise no fixpoint may be detected and
 * compilation runs into an endless loop.
 * 
 * @author moritzlichter
 *
 */
public class FixpointCycleSupport implements CycleSupport {

  public static final CycleSupportFactory of(BuilderFactory<?, ?, ?>... builders) {
    return (BuildCycle cycle) -> new FixpointCycleSupport(cycle, builders);
  }

  /**
   * All BuilderFactories which are supported
   */
  private final List<BuilderFactory<?, ?, ?>> supportedBuilders;
  /**
   * The cycle to compile
   */
  private final BuildCycle cycle;

  /**
   * Creates a new {@link FixpointCycleSupport} which is able to handle build
   * requests to builders created by the given factories. If the cycle contains
   * any builder which is not from the given factories, the support rejects to
   * compile the cycle.
   * 
   * @param supportedBuilders
   *          all supported builder factories
   */
  public FixpointCycleSupport(BuildCycle cycle, BuilderFactory<?, ?, ?>... supportedBuilders) {
    this.cycle = cycle;
    this.supportedBuilders = Arrays.asList(supportedBuilders);
  }

  @Override
  public String cycleDescription() {
    String descriptions = cycle.getCycleComponents().stream().map((BuildRequest<?, ?, ?, ?> r) -> r.createBuilder()).map(Builder::description).reduce((String desc1, String desc2) -> desc1 + "; " + desc2).get();
    return "Fixpoint {" + descriptions + "}";
  }

  @Override
  public boolean canBuildCycle() {
    // Each builder in the cycle must be supported
    Predicate<BuildRequest<?, ?, ?, ?>> buildRequestSupported = (BuildRequest<?, ?, ?, ?> r) -> supportedBuilders.contains(r.factory);
    return cycle.getCycleComponents().stream().allMatch(buildRequestSupported);
  }

  @Override
  public Set<BuildUnit<?>> buildCycle(BuildUnitProvider manager) throws Throwable {
    FixpointCycleBuildResultProvider cycleManager = new FixpointCycleBuildResultProvider(manager, cycle);

    int numInterations = 1;
    boolean cycleConsistent = false;
    List<BuildRequest<?, ?, ?, ?>> cyclicRequests = cycle.getCycleComponents();

    // Compile the cycle again and again until the fixpoint is detected. The
    // fixpoint is reached if
    // the complete cycle is consistent, so during an iteration no builder
    // executed
    while (!cycleConsistent) {
      Log.log.beginTask("Fixpoint interation " + numInterations, Log.CORE);
      cycleManager.startNextIteration();
      try {
        // CycleComponents are in order if which they were required
        // Require the first one which is not consistent to their input
        for (BuildRequest<?, ?, ?, ?> req : cyclicRequests) {
          cycleManager.require(req, true);
        }
        cycleConsistent = !cycleManager.wasAnyBuilderExecutedInIteration();
      } catch (RequiredBuilderFailed e) {
        throw e;
      }
      Log.log.endTask();
      numInterations++;
    }
    Log.log.log("Fixpoint detected.", Log.CORE);
    return cycleManager.getAllUnitsInCycle();
  }

}
