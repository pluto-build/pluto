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
  
  /**
   * The list of the stack entries which form a dependency cycle in order of the stack.
   */
  private final BuildStackEntry cycleCause;
  private final List<BuildRequirement<?>> cycleComponents;
  
  public BuildCycleException(String message, BuildStackEntry cycleCause) {
    super(message);
    Objects.requireNonNull(cycleCause);
    this.cycleCause = cycleCause;
    this.cycleComponents  = new ArrayList<>();
  }
  
  public List<BuildRequirement<?>> getCycleComponents() {
    return cycleComponents;
  }
  
  public void addCycleComponent(BuildRequirement<?> req) {
    cycleComponents.add(0, req);
  }
  
  public boolean isUnitFirstInvokedOn(Path path, BuilderFactory<?, ?, ?> factory) {
    return cycleCause.getPersistencePath().equals(path) && cycleCause.getRequest().factory.equals(factory);
  }
  
  

}
