package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.common.path.Path;

public class BuildStackEntry<Out extends Serializable> {
  private final BuildUnit<Out> unit;
  private final Path persistencePath;
  
  public BuildStackEntry(BuildUnit<Out> unit) {
    super();this.unit = unit;
    this.persistencePath = unit.getPersistentPath();
  }
  public BuildUnit<Out> getUnit() {
    return unit;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((persistencePath == null) ? 0 : persistencePath.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BuildStackEntry other = (BuildStackEntry) obj;
    if (persistencePath == null) {
      if (other.persistencePath != null)
        return false;
    } else if (!persistencePath.equals(other.persistencePath))
      return false;
    return true;
  }
 
  
  
}