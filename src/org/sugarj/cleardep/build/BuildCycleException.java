package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.common.path.Path;
import org.sugarj.common.util.Pair;

public class BuildCycleException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = -8981404220171314788L;
  
  /**
   * The list of the stack entries which form a dependency cycle in order of the stack.
   */
  private final BuildStackEntry cycleCause;
  private final List<Pair<BuildUnit, BuildRequest<?,?,?,?>>> cycleComponents;
  
  public BuildCycleException(String message, BuildStackEntry cycleCause) {
    super(message);
    Objects.requireNonNull(cycleCause);
    this.cycleCause = cycleCause;
    this.cycleComponents  = new ArrayList<>();
  }
  
  public List<Pair<BuildUnit, BuildRequest<?, ?, ?, ?>>> getCycleComponents() {
    return cycleComponents;
  }
  
  public void addCycleComponent(Pair<BuildUnit, BuildRequest<?, ?, ?, ?>> component) {
    cycleComponents.add(0, component);
  }
  
  public boolean isUnitFirstInvokedOn(Path path, BuilderFactory<?, ?, ?> factory) {
    return cycleCause.getPersistencePath().equals(path) && cycleCause.getRequest().factory.equals(factory);
  }
  
  

}
