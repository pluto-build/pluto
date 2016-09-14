package build.pluto.dependency.database;

import java.io.ByteArrayInputStream;
import java.io.File;

import jetbrains.exodus.bindings.ComparableBinding;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.util.LightOutputStream;

public class FileByteIterableBinding extends ComparableBinding {

  public static final FileByteIterableBinding BINDING = new FileByteIterableBinding();
  
  private FileByteIterableBinding() {
  }
  
  @Override
  public File readObject(ByteArrayInputStream stream) {
    return new File(StringBinding.BINDING.readObject(stream));
  }

  @Override
  public void writeObject(LightOutputStream output, Comparable object) {
    StringBinding.BINDING.writeObject(output, ((File) object).getPath());
  }

}
