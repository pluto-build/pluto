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
public class FileContentStamper implements Stamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private FileContentStamper() {}
  public static final Stamper instance = new FileContentStamper();
  
  /**
   * @see build.pluto.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(File p) {
    if (!p.exists())
      return new ValueStamp<>(this, new byte[0]);
    
    if (p.isDirectory()) {
      Map<File, Stamp> stamps = new HashMap<>();
      stamps.put(p, new ValueStamp<>(this,p.lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p.toPath()))
        if (Files.isDirectory(sub))
          stamps.put(sub.toFile(), new ValueStamp<>(this, sub.toFile().lastModified()));
        else
          stamps.put(sub.toFile(), fileContentStamp(sub));
      
      return new ValueStamp<>(this, stamps);
    }
    
    return fileContentStamp(p.toPath());
  }

  private Stamp fileContentStamp(Path p) {
    try {
      return new ByteArrayStamp(this, Files.readAllBytes(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ByteArrayStamp(this, null);
    }
  }
}
