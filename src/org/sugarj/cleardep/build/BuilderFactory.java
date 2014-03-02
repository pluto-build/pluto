package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;

public interface BuilderFactory
<
  T extends Serializable, 
  E extends BuildUnit, 
  B extends Builder<T, E>
> extends Serializable {
  public B makeBuilder(T input, BuildManager manager);
}
