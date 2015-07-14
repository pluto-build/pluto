package build.pluto.dependency;

import java.io.File;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.stamp.Stamp;

public class FileRequirement implements Requirement {
  private static final long serialVersionUID = -8539311813637744518L;
  
  public final File file;
  public final Stamp stamp;
  
  public FileRequirement(File file, Stamp stamp) {
    this.file = file;
    this.stamp = stamp;
  }
  
  @Override
  public boolean isConsistent() {
    return stamp.equals(stamp.getStamper().stampOf(file));
  }
  
  @Override
  public boolean tryMakeConsistent(BuildUnitProvider manager) {
    return isConsistent();
  }
  
  @Override
  public String toString() {
    return "FileReq(" + file.toString() + ")";
  }
}
