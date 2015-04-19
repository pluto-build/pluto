package build.pluto.output;

import java.io.Serializable;

import com.google.common.base.Objects;

public class MemoryOutput<T> implements Serializable {

  private static final long serialVersionUID = 1145504558447893647L;

  public final transient T output;
  
  public MemoryOutput(T output) {
    this.output = output;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof MemoryOutput && Objects.equal(output, ((MemoryOutput<?>) obj).output);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(output);
  }
}
