package org.sugarj.cleardep.stamp;

import java.io.Serializable;

public interface ModuleStamp extends Serializable {
  public boolean equals(ModuleStamp o);
  public ModuleStamper getModuleStamper();
}
