package org.sugarj.cleardep.output;

import java.io.Serializable;

public interface OutputStamp extends Serializable {
  public boolean equals(Object o);
  public OutputStamper<Serializable> getStamper();
}
