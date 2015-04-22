package build.pluto.builder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sugarj.common.Log;

import build.pluto.dependency.BuildRequirement;

public class BuildCycle {

  private Set<BuildRequirement<?>> cycle;

  public BuildCycle(Set<BuildRequirement<?>> cycleComponents) {
    super();
    this.cycle = cycleComponents;
  }

  public Set<BuildRequirement<?>> getCycleComponents() {
    return cycle;
  }

  protected Optional<CycleSupport> findCycleSupport() {
    List<CycleSupport> matchingSupports = this.cycle.stream().map((BuildRequirement<?> req) -> req.getRequest().createBuilder().getCycleSupport()).filter((CycleSupport c) -> c != null && c.canCompileCycle(this)).collect(Collectors.toList());

    if (matchingSupports.size() > 1) {
      Log.log.log("Found " + matchingSupports.size() + " matching cycle supports for cycle.", Log.CORE);
    }

    return matchingSupports.stream().findAny();
  }

}
