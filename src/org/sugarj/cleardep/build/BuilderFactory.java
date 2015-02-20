package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;

public interface BuilderFactory
<
  C extends BuildContext, 
  T extends Serializable, 
  E extends CompilationUnit, 
  B extends Builder<C, T, E>
> extends Serializable {
  
  public B makeBuilder(C context);
}
