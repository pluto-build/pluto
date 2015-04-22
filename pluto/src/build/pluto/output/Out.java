package build.pluto.output;

import java.io.Serializable;

import com.google.common.base.Objects;

public class Out<T extends Serializable> implements Output {
  private static final long serialVersionUID = -897559877130499097L;
  
  public final T val;
  public Out(T val) {
    this.val = val;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Out && Objects.equal(val, ((Out<?>) obj).val);
  }
  
  @Override
  public int hashCode() {
    return val.hashCode();
  }
  
  @Override
  public String toString() {
    return "Out(" + val + ")";
  }
}
