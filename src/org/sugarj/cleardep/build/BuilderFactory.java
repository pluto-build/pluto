package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;

public interface BuilderFactory
<
  T extends Serializable, 
  E extends CompilationUnit, 
  B extends Builder<T, E>
> extends Serializable {
  public B makeBuilder(T input, BuildManager manager);
}
