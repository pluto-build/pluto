package org.sugarj.cleardep.stamp;

import java.io.Serializable;

public interface Stamp extends Serializable {
  public boolean equals(Stamp o);
  public Stamper getStamper();
}
