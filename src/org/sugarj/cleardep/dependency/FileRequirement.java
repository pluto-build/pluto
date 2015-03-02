package org.sugarj.cleardep.dependency;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

public class FileRequirement implements Requirement {
  private static final long serialVersionUID = -8539311813637744518L;
  
  public final Path path;
  public final Stamp stamp;
  
  public FileRequirement(Path path, Stamp stamp) {
    this.path = path;
    this.stamp = stamp;
  }
  
  @Override
  public boolean isConsistent() {
    return stamp.equals(stamp.getStamper().stampOf(path));
  }
  
  @Override
  public String toString() {
    return "FileReq(" + FileCommands.tryGetRelativePath(path) + ")";
  }
}
