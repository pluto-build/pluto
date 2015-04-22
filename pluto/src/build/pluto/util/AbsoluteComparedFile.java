package build.pluto.util;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class AbsoluteComparedFile implements Externalizable {
  
  private File file;
  private File absoluteFile;
  
  public static AbsoluteComparedFile absolute(File file) {
    return new AbsoluteComparedFile(file);
  }
  
  public static boolean equals(File file1, File file2) {
    return file1.getAbsoluteFile().equals(file2.getAbsoluteFile());
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

  @Override
  public String toString() {
    return "ABS[" + file.toString() + "]";
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(file);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.file = (File) in.readObject();
    this.absoluteFile = this.file.getAbsoluteFile();
  }

}
