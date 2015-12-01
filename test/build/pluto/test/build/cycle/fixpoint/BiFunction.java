package build.pluto.test.build.cycle.fixpoint;

public interface BiFunction<A, B, Res> {
  public Res apply(A a, B b);
}
