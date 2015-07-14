package build.pluto.test.build.cycle.fixpoint;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.builder.FixpointCycleHandler;

public class NumericCycleSupport extends FixpointCycleHandler {

  public static final CycleHandlerFactory factory = NumericCycleSupport::new;

  public NumericCycleSupport(BuildCycle cycle) {
    super(cycle, GCDBuilder.factory, ModuloBuilder.factory, DivideByBuilder.factory);
  }

}
