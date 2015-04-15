package build.pluto.dependency;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.stamp.Stamp;

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
  public boolean isConsistentInBuild(BuildUnitProvider manager) {
    return isConsistent();
  }
  
  @Override
  public String toString() {
    return "FileReq(" + FileCommands.tryGetRelativePath(path) + ")";
  }
}
