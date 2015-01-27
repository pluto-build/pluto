package org.sugarj.cleardep.stamp;

import java.io.Serializable;

public interface ModuleStamp extends Serializable {
  public boolean equals(Object o);
  public ModuleStamper getModuleStamper();
}
