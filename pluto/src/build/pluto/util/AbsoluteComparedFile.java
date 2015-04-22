package build.pluto.util;

import java.io.File;
import java.util.Objects;

public class AbsoluteComparedFile {
  
  private File file;
  private File absoluteFile;
  
  public static AbsoluteComparedFile absolute(File file) {
    return new AbsoluteComparedFile(file);
  }
  
  public AbsoluteComparedFile(File file) {
    super();
    Objects.requireNonNull(file);
    this.file = file;
    this.absoluteFile = this.file.getAbsoluteFile();
  }
  
  public File getFile() {
    return file;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((absoluteFile == null) ? 0 : absoluteFile.hashCode());
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
    AbsoluteComparedFile other = (AbsoluteComparedFile) obj;
    if (absoluteFile == null) {
      if (other.absoluteFile != null)
        return false;
    } else if (!absoluteFile.equals(other.absoluteFile))
      return false;
    return true;
  }

}
