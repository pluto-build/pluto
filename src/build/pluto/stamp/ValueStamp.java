package build.pluto.stamp;

import java.util.Objects;

public class ValueStamp<T> implements Stamp {
  private static final long serialVersionUID = 3600975022352030761L;

  public final Stamper stamper;
  public final T val;

  public ValueStamp(Stamper stamper, T val) {
    this.stamper = stamper;
    this.val = val;
  }

  @Override
  public Stamper getStamper() {
    return stamper;
  }

  @Override
  public boolean equals(Stamp obj) {
    return equals((Object) obj);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("rawtypes")
    final ValueStamp other = (ValueStamp) obj;
    return Objects.equals(val, other.val);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(val);
  }
}
