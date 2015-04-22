package build.pluto.output;

import java.io.Serializable;

public class Out<T extends Serializable> implements Output {
  private static final long serialVersionUID = -897559877130499097L;
  
  public final T val;
  public Out(T val) {
    this.val = val;
  }
}
