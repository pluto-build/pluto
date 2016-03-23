package build.pluto.stamp;

import java.util.Arrays;

public class ByteArrayStamp implements Stamp {
  private static final long serialVersionUID = 3600975022352030761L;

  public final Stamper stamper;
  public final byte[] val;

  public ByteArrayStamp(Stamper stamper, byte[] val) {
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
    final ByteArrayStamp other = (ByteArrayStamp) obj;
    return Arrays.equals(val, other.val);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(val);
  }
}
