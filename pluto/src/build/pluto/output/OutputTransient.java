package build.pluto.output;

import java.io.Serializable;
import java.util.Objects;

public class OutputTransient<T> implements Out<T> {
  private static final long serialVersionUID = -2569694506974721681L;
  
  public final transient T val;
  public OutputTransient(T val) {
    this.val = val;
  }
  
  public T val() { 
    return val; 
  }
  
  public static <T extends Serializable> OutputTransient<T> of(T val) {
    return new OutputTransient<>(val);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OutputTransient && Objects.equals(val, ((OutputTransient<?>) obj).val);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(val);
  }
  
  @Override
  public String toString() {
    return "OutInMemory(" + val + ")";
  }
}
