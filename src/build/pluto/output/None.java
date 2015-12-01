package build.pluto.output;


public class None implements Output {

  private static final long serialVersionUID = -4427485206308344655L;
  
  public static None val = new None();
  
  public None() {}
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof None;
  }
  
  @Override
  public int hashCode() {
    return 0;
  }
}
