package build.pluto.output;

import java.io.Serializable;
import java.util.Objects;

import com.cedarsoftware.util.DeepEquals;

/**
 * Use for outputs that should or can not be serialized to disk.
 * Instead of the object, this class uses the deep hash code for checking consistency.
 */
public class OutputTransient<T> implements Out<T> {
  private static final long serialVersionUID = -2569694506974721681L;
  
  private transient final boolean constructed;
  private final transient T val;
  private int hash;
  
  public OutputTransient(T val) {
    this.val = val;
    this.hash = DeepEquals.deepHashCode(val);
    this.constructed = true;
  }
  
  public T val() { 
    return val; 
  }
  
  public static <T extends Serializable> OutputTransient<T> of(T val) {
    return new OutputTransient<>(val);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OutputTransient<?>) {
      OutputTransient<?> o = (OutputTransient<?>) obj;
      if (constructed && o.constructed)
        return Objects.equals(val, o.val);
      else
        return hash == o.hash;
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return hash;
  }
  
  @Override
  public String toString() {
    return "OutputTransient(" + val + ")";
  }

  @Override
  public boolean expired() {
    return !constructed;
  }
}
