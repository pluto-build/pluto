package org.sugarj.cleardep.build;

import org.sugarj.cleardep.CompilationUnit;

public interface BuilderFactory
<
  T, 
  E extends CompilationUnit, 
  B extends Builder<T, E>
> {
  public B makeBuilder(T input);
}
