package org.sugarj.cleardep.build;

import java.io.Serializable;

public interface BuilderFactory
<
  In extends Serializable, 
  Out extends Serializable, 
  B extends Builder<In, Out>
> extends Serializable {
  public B makeBuilder(In input);
}
