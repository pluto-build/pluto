package org.sugarj.cleardep.output;

import java.io.Serializable;

public interface BuildOutput extends Serializable {
  public boolean isConsistent();
}
