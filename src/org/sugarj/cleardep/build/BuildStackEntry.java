package org.sugarj.cleardep.build;

import org.sugarj.common.path.Path;

class BuildStackEntry {
  private final BuildRequest<?, ?, ?, ?> req;
  private final Path persistencePath;
  
  
  public BuildStackEntry(BuildRequest<?, ?, ?,?> req, Path persistencePath) {
    super();
    this.req = req;
    this.persistencePath = persistencePath;
  }
  
  public BuildRequest<?, ?, ?, ?> getRequest() {
    return req;
  }
  public Path getPersistencePath() {
    return persistencePath;
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