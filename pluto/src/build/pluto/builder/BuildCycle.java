package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sugarj.common.Log;

public class BuildCycle {

  private BuildRequest<?, ?, ?, ?> initial;
  private List<BuildRequest<?, ?, ?, ?>> cycle;

  public BuildCycle(BuildRequest<?, ?, ?, ?> initial, List<BuildRequest<?, ?, ?, ?>> cycleComponents) {
    super();
    this.initial = initial;
    this.cycle = new ArrayList<>(cycleComponents);
  }

  public List<BuildRequest<?, ?, ?, ?>> getCycleComponents() {
    return cycle;
  }

  public BuildRequest<?, ?, ?, ?> getInitial() {
    return initial;
  }

  protected Optional<CycleSupport> findCycleSupport() {
    List<CycleSupportFactory> matchingSupports = this.cycle.stream().map((BuildRequest<?, ?, ?, ?> req) -> req.createBuilder().getCycleSupport()).filter((CycleSupportFactory c) -> c != null && c.createCycleSupport(this).canCompileCycle()).collect(Collectors.toList());

    if (matchingSupports.size() > 1) {
      Log.log.log("Found " + matchingSupports.size() + " matching cycle supports for cycle.", Log.CORE);
    }

    return matchingSupports.stream().findAny().map((CycleSupportFactory f) -> f.createCycleSupport(this));
  }

}
