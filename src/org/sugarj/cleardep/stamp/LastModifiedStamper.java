package org.sugarj.cleardep.stamp;

import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 *
 */
public class LastModifiedStamper implements Stamper {

  private static final long serialVersionUID = 8242859577253542194L;

  private LastModifiedStamper() {}
  public static final Stamper instance = new LastModifiedStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ValueStamp<>(this, 0l);
    
    if (p.getFile().isDirectory()) {
      Map<Path, Stamp> stamps = new HashMap<>();
      stamps.put(p, stampOf(p));
      
      for (Path sub : FileCommands.listFilesRecursive(p))
        stamps.put(sub, stampOf(sub));
      
      return new ValueStamp<>(this, stamps);
    }
    
    return new ValueStamp<>(this, p.getFile().lastModified());
  }
}
