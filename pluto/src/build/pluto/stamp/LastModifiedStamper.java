package build.pluto.stamp;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;

/**
 * @author Sebastian Erdweg
 *
 */
public class LastModifiedStamper implements Stamper {

  private static final long serialVersionUID = 8242859577253542194L;

  private LastModifiedStamper() {}
  public static final Stamper instance = new LastModifiedStamper();
  
  /**
   * @see build.pluto.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(File p) {
    if (!p.exists())
      return new ValueStamp<>(this, 0l);
    
    if (p.isDirectory()) {
      Map<File, Stamp> stamps = new HashMap<>();
      stamps.put(p, new ValueStamp<>(this, p.lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p.toPath()))
        stamps.put(sub.toFile(), new ValueStamp<>(this, sub.toFile().lastModified()));
      
      return new ValueStamp<>(this, stamps);
    }
    
    return new ValueStamp<>(this, p.lastModified());
  }
}
