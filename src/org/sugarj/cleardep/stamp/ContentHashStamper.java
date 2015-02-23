package org.sugarj.cleardep.stamp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.cleardep.stamp.CollectionStamper.CollectionStamp;
import org.sugarj.cleardep.stamp.LastModifiedStamper.LastModifiedStamp;
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
      return new ContentHashStamp(0);
    
    if (p.getFile().isDirectory()) {
      Map<Path, Stamp> stamps = new HashMap<>();
      stamps.put(p, new LastModifiedStamp(p.getFile().lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p))
        if (sub.getFile().isDirectory())
          stamps.put(sub, new LastModifiedStamp(sub.getFile().lastModified()));
        else
          stamps.put(sub, fileContentHashStamp(sub));
      
      return new CollectionStamp(stamps, this);
    }
    
    return fileContentHashStamp(p);
  }

  private ContentHashStamp fileContentHashStamp(Path p) {
    try {
      return new ContentHashStamp(FileCommands.fileHash(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ContentHashStamp(-1);
    }
  }

  public static class ContentHashStamp implements Stamp {

    private static final long serialVersionUID = 7535020621495360152L;
    
    private final Integer value;

    public ContentHashStamp(Integer value) {
      this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof ContentHashStamp) {
        Integer ovalue = ((ContentHashStamp) o).value;
        return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
      }
      if (o instanceof ContentHashStamp) {
        Integer ovalue = ((ContentHashStamp) o).value;
        return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
      }
      return false;
    }
    
    @Override
    public Stamper getStamper() {
      return ContentHashStamper.instance;
    }
    
    @Override
    public String toString() {
      return "ContentHash(" + value + ")";
    }
  }
}
