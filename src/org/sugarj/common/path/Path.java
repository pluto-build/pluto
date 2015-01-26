package org.sugarj.common.path;

import java.io.Externalizable;
import java.io.File;

/**
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public abstract class Path implements Externalizable {

  public abstract String getAbsolutePath();
  
  public File getFile() {
    return new File(getAbsolutePath());
  }
  
  public String toString() {
    return getAbsolutePath();
  }
  
  public int hashCode() {
    return getAbsolutePath().hashCode();
  }
  
  public boolean equals(Object o) {
    return o instanceof Path && ((Path) o).getAbsolutePath().equals(getAbsolutePath());
  }

  protected String trimFront(String path) {
    while (path.startsWith(File.separator))
      path = path.substring(1, path.length());
    return path;
  }
  
  protected String trimBack(String path) {
    while (path.endsWith(File.separator))
      path = path.substring(0, path.length() - 1);
    return path;
  }
}
