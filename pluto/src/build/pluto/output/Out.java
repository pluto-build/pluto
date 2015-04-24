package build.pluto.output;

import java.io.Serializable;

import com.google.common.base.Objects;

public class Out<T extends Serializable> implements Output {
  private static final long serialVersionUID = -897559877130499097L;
  
  public final T val;
  public Out(T val) {
    this.val = val;
  }
  
  public static <T extends Serializable> Out<T> of(T val) {
    return new Out<>(val);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Out && Objects.equal(val, ((Out<?>) obj).val);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(val);
  }
  
  @Override
  public String toString() {
    return "Out(" + val + ")";
  }
}
