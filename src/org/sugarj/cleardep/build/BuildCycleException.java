package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sugarj.cleardep.CompilationUnit;
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
  private final List<Pair<CompilationUnit, BuildRequirement<?,?,?,?>>> cycleComponents;
  private boolean lastCallAborted = false;
  
  public BuildCycleException(String message, BuildStackEntry cycleCause) {
    super(message);
    Objects.requireNonNull(cycleCause);
    this.cycleCause = cycleCause;
    this.cycleComponents  = new ArrayList<>();
  }
  
  public List<Pair<CompilationUnit, BuildRequirement<?, ?, ?, ?>>> getCycleComponents() {
    return cycleComponents;
  }
  
  public void addCycleComponent(Pair<CompilationUnit, BuildRequirement<?, ?, ?, ?>> component) {
    cycleComponents.add(0, component);
  }
  
  public boolean isUnitForstInvokedOn(Path path, BuilderFactory<?, ?, ?> factory) {
    return cycleCause.getPersistencePath().equals(path) && cycleCause.getRequirement().factory.equals(factory);
  }
  
  public void setLastCallAborted(boolean lastCallAborted) {
    this.lastCallAborted = lastCallAborted;
  }
  
  public boolean isLastCallAborted() {
    return lastCallAborted;
  }

}
