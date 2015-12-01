package build.pluto.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  protected CycleHandler findCycleSupport() {
    Set<CycleHandlerFactory> matchingSupports = new HashSet<>();
    for (BuildRequest<?, ?, ?, ?> req : this.cycle) {
      CycleHandlerFactory c = req.createBuilder().getCycleSupport();
      if (c != null && c.createCycleSupport(this).canBuildCycle())
        matchingSupports.add(c);
    }
      
//    if (matchingSupports.size() > 1) {
//      Log.log.log("Found " + matchingSupports.size() + " matching cycle supports for cycle.", Log.CORE);
//    }

    if (matchingSupports.isEmpty())
      return null;
    return matchingSupports.iterator().next().createCycleSupport(this);
  }
  
  public String description() {
    StringBuilder builder = new StringBuilder();
    for (BuildRequest<?, ?, ?, ?> r : getCycleComponents())
      builder.append(r.createBuilder().description() + ", ");
    String descriptions = builder.toString();
    if (!descriptions.isEmpty())
      descriptions.substring(0,  descriptions.length() - 2);
    return descriptions;
  }
}
