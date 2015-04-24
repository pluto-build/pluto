package build.pluto.test.build.cycle.fixpoint;

import build.pluto.builder.FixpointCycleSupport;

public class NumericCycleSupport extends FixpointCycleSupport {

  public NumericCycleSupport() {
    super(GCDBuilder.factory, ModuloBuilder.factory, DivideByBuilder.factory);
  }

}
