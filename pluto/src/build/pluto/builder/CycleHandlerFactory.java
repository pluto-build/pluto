package build.pluto.builder;

public interface CycleHandlerFactory {

  public CycleHandler createCycleSupport(BuildCycle cycle);

}
