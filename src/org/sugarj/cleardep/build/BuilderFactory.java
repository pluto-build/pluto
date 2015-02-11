package org.sugarj.cleardep.build;

import org.sugarj.cleardep.CompilationUnit;

public interface BuilderFactory
<
  C extends BuildContext, 
  T, 
  E extends CompilationUnit, 
  B extends Builder<C, T, E>
> {
  public B makeBuilder(C context);
}
