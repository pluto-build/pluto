package org.sugarj.cleardep.output;

import java.io.Serializable;

public interface OutputStamp<Out extends Serializable> extends Serializable {
  public boolean equals(Object o);
  public OutputStamper<Out> getStamper();
}
