package org.sugarj.cleardep.stamp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.cleardep.stamp.CollectionStamper.CollectionStamp;
import org.sugarj.cleardep.stamp.LastModifiedStamper.LastModifiedStamp;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class ContentStamper implements Stamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private ContentStamper() {}
  public static final Stamper instance = new ContentStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ContentStamp(new byte[0]);
    
    if (p.getFile().isDirectory()) {
      Map<Path, Stamp> stamps = new HashMap<>();
      stamps.put(p, new LastModifiedStamp(p.getFile().lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p))
        if (sub.getFile().isDirectory())
          stamps.put(sub, new LastModifiedStamp(sub.getFile().lastModified()));
        else
          stamps.put(sub, fileContentStamp(sub));
      
      return new CollectionStamp(stamps, this);
    }
    
    return fileContentStamp(p);
  }

  private ContentStamp fileContentStamp(Path p) {
    try {
      return new ContentStamp(FileCommands.readFileAsByteArray(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ContentStamp(new byte[0]);
    }
  }

  public static class ContentStamp implements Stamp {

    private static final long serialVersionUID = 7535020621495360152L;
    
    private final byte[] value;

    public ContentStamp(byte[] value) {
      this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof ContentStamp) {
        byte[] ovalue = ((ContentStamp) o).value;
        return ovalue == null && value == null || Arrays.equals(ovalue, value);
      }
      if (o instanceof ContentStamp) {
        byte[] ovalue = ((ContentStamp) o).value;
        return ovalue == null && value == null || Arrays.equals(ovalue, value);
      }
      return false;
    }
    
    @Override
    public Stamper getStamper() {
      return ContentStamper.instance;
    }
    
    @Override
    public String toString() {
      return "ContentHash(byte[" + value.length + "])";
    }
  }
}
