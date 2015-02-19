package org.sugarj.cleardep.build;

import org.sugarj.common.path.Path;

class BuildStackEntry {
  private final Builder<?, ?, ?> builder;
  private final Path persistencePath;
  
  
  public BuildStackEntry(Builder<?, ?, ?> builder, Path persistencePath) {
    super();
    this.builder = builder;
    this.persistencePath = persistencePath;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((builder == null) ? 0 : builder.hashCode());
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
    if (builder == null) {
      if (other.builder != null)
        return false;
    } else if (!builder.equals(other.builder))
      return false;
    if (persistencePath == null) {
      if (other.persistencePath != null)
        return false;
    } else if (!persistencePath.equals(other.persistencePath))
      return false;
    return true;
  }

  
  
}