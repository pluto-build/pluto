package build.pluto.builder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sugarj.common.Log;

public class BuildCycle {

  private List<BuildRequest<?, ?, ?, ?>> cycle;

  public BuildCycle(List<BuildRequest<?, ?, ?, ?>> cycleComponents) {
    super();
    this.cycle = cycleComponents;
  }

  public List<BuildRequest<?, ?, ?, ?>> getCycleComponents() {
    return cycle;
  }

  protected Optional<CycleSupport> findCycleSupport() {
    List<CycleSupport> matchingSupports = this.cycle.stream().map((BuildRequest<?, ?, ?, ?> req) -> req.createBuilder().getCycleSupport()).filter((CycleSupport c) -> c != null && c.canCompileCycle(this)).collect(Collectors.toList());

    if (matchingSupports.size() > 1) {
      Log.log.log("Found " + matchingSupports.size() + " matching cycle supports for cycle.", Log.CORE);
    }

    return matchingSupports.stream().findAny();
  }

}
