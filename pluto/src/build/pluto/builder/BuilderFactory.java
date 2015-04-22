package build.pluto.builder;

import java.io.Serializable;

import build.pluto.output.Output;

public interface BuilderFactory
<
  In extends Serializable, 
  Out extends Output, 
  B extends Builder<In, Out>
> extends Serializable {
  public B makeBuilder(In input);
}
