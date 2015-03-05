package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.output.BuildOutput;

public interface BuilderFactory
<
  In extends Serializable, 
  Out extends BuildOutput, 
  B extends Builder<In, Out>
> extends Serializable {
  public B makeBuilder(In input);
}
