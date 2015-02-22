package org.sugarj.cleardep.build;

import org.sugarj.common.path.Path;

class BuildStackEntry {
  private final BuilderFactory<?, ?, ?> factory;
  private final Path persistencePath;
  
  
  public BuildStackEntry(BuilderFactory<?, ?, ?> factory, Path persistencePath) {
    super();
    this.factory = factory;
    this.persistencePath = persistencePath;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((factory == null) ? 0 : factory.hashCode());
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
    if (factory == null) {
      if (other.factory != null)
        return false;
    } else if (!factory.equals(other.factory))
      return false;
    if (persistencePath == null) {
      if (other.persistencePath != null)
        return false;
    } else if (!persistencePath.equals(other.persistencePath))
      return false;
    return true;
  }

  
  
}