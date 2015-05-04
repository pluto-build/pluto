package build.pluto.builder;

@FunctionalInterface
public interface CycleSupportFactory {

  public CycleSupport createCycleSupport(BuildCycle cycle);

}
