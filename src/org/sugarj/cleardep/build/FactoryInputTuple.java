package org.sugarj.cleardep.build;

import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.CompilationUnit;

public final class FactoryInputTuple 
<
C extends BuildContext, 
T extends Serializable, 
E extends CompilationUnit, 
B extends Builder<C, T, E>,
F extends BuilderFactory<C, T, E, B>
> implements Serializable{
  
  /**
   * 
   */
  private static final long serialVersionUID = -5508395031214216201L;
  
  private F factory;
  private Class<F> factoryClass;
  private T input;
  private Class<T> inputClass;
  
  public FactoryInputTuple(F factory, Class<F> factoryClass, T input, Class<T> inputClass) {
    super();
    Objects.requireNonNull(input);
    Objects.requireNonNull(factory);
    Objects.requireNonNull(factoryClass);
    Objects.requireNonNull(inputClass);
    this.factory = factory;
    this.input = input;
    this.factoryClass = factoryClass;
    this.inputClass = inputClass;
  }
  
  public F getFactory() {
    return factory;
  }
  
  public Class<F> getFactoryClass() {
    return factoryClass;
  }
  
  public T getInput() {
    return input;
  }
  
  public Class<T> getInputClass() {
    return inputClass;
  }

}
