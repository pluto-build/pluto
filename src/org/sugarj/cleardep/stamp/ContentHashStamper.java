package org.sugarj.cleardep.stamp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class ContentHashStamper implements Stamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private ContentHashStamper() {}
  public static final Stamper instance = new ContentHashStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ValueStamp<>(this, null);
    
    if (p.getFile().isDirectory()) {
      Map<Path, Stamp> stamps = new HashMap<>();
      stamps.put(p, new ValueStamp<>(this, p.getFile().lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p))
        if (sub.getFile().isDirectory())
          stamps.put(sub, new ValueStamp<>(this, sub.getFile().lastModified()));
        else
          stamps.put(sub, fileContentHashStamp(sub));
      
      return new ValueStamp<>(this, stamps);
    }
    
    return fileContentHashStamp(p);
  }

  private Stamp fileContentHashStamp(Path p) {
//    String[][] msgs = new CommandExecution(true).execute("openssl", "sha1", p.getAbsolutePath());
//    String line = msgs[0][0];
//    String digest = line.substring(line.indexOf('=') + 1);
//    return new ValueStamp<>(this, digest);
    try {
      return new ValueStamp<>(this, FileCommands.fileHash(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ValueStamp<>(this, -1);
    }
  }
}
