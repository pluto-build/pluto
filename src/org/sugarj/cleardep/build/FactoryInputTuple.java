package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;

public final class FactoryInputTuple 
<
T extends Serializable, 
E extends CompilationUnit, 
B extends Builder<T, E>,
F extends BuilderFactory<T, E, B>
> implements Serializable{
  
  /**
   * 
   */
  private static final long serialVersionUID = -5508395031214216201L;
  
  private F factory;
  private T input;
  private Mode<E> mode;
  
  public FactoryInputTuple(F factory, T input,  Mode<E> mode) {
    super();
    Objects.requireNonNull(input);
    Objects.requireNonNull(factory);
    Objects.requireNonNull(mode);
    this.factory = factory;
    this.input = input;
    this.mode = mode;
  }
  
  public F getFactory() {
    return factory;
  }
  
  
  public T getInput() {
    return input;
  }
  
  public E createBuilderAndRequire(BuildManager manager) throws IOException {
    return manager.require(factory.makeBuilder(input, manager), mode);
  }

}
