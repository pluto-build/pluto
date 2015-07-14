package build.pluto.builder;

@FunctionalInterface
public interface CycleHandlerFactory {

  public CycleHandler createCycleSupport(BuildCycle cycle);

}
