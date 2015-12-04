package build.pluto.output;

import java.util.Objects;

public class OutputPersisted<T> implements Out<T> {
  private static final long serialVersionUID = -897559877130499097L;
  
  public final T val;
  public OutputPersisted(T val) {
    this.val = val;
  }
  
  public T val() { 
    return val; 
  }
  
  public static <T> OutputPersisted<T> of(T val) {
    return new OutputPersisted<>(val);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OutputPersisted && Objects.equals(val, ((OutputPersisted<?>) obj).val);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(val);
  }
  
  @Override
  public String toString() {
    return "OutputPersisted(" + val + ")";
  }

  @Override
  public boolean expired() {
    return false;
  }
}
