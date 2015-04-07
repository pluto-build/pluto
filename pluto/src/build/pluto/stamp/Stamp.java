package build.pluto.stamp;

import java.io.Serializable;

public interface Stamp extends Serializable {
  public boolean equals(Object o);
  public Stamper getStamper();
}
