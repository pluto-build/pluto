package org.sugarj.cleardep.dependency;

import java.io.Serializable;

public interface Requirement extends Serializable {
  public boolean isConsistent();
}
