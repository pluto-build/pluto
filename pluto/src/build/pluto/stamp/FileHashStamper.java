package build.pluto.stamp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;

/**
 * @author Sebastian Erdweg
 */
public class FileHashStamper implements Stamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private FileHashStamper() {}
  public static final Stamper instance = new FileHashStamper();
  
  /**
   * @see build.pluto.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(File p) {
    if (!p.exists())
      return new ValueStamp<>(this, null);
    
    if (p.isDirectory()) {
      Map<File, Stamp> stamps = new HashMap<>();
      stamps.put(p, new ValueStamp<>(this, p.lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p.toPath()))
        if (Files.isDirectory(sub))
          stamps.put(sub.toFile(), new ValueStamp<>(this, sub.toFile().lastModified()));
        else
          stamps.put(sub.toFile(), fileContentHashStamp(sub.toFile()));
      
      return new ValueStamp<>(this, stamps);
    }
    
    return fileContentHashStamp(p);
  }

  private Stamp fileContentHashStamp(File p) {
    try {
      return new ByteArrayStamp(this, FileCommands.fileHash(p.toPath()));
    } catch (IOException e) {
      e.printStackTrace();
      return new ValueStamp<>(this, -1);
    }
  }
}
