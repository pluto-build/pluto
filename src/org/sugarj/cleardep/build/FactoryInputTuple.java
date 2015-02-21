package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;

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
  private Mode<E> mode;
  
  public FactoryInputTuple(F factory, Class<F> factoryClass, T input, Class<T> inputClass, Mode<E> mode) {
    super();
    Objects.requireNonNull(input);
    Objects.requireNonNull(factory);
    Objects.requireNonNull(factoryClass);
    Objects.requireNonNull(inputClass);
    Objects.requireNonNull(mode);
    this.factory = factory;
    this.input = input;
    this.factoryClass = factoryClass;
    this.inputClass = inputClass;
    this.mode = mode;
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
  
  public E createBuilderAndRequire(C context) throws IOException {
    return factory.makeBuilder(context).require(input, mode);
  }

}
