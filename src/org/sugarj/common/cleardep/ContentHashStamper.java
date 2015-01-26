package org.sugarj.common.cleardep;

import java.io.IOException;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class ContentHashStamper implements Stamper {

  private ContentHashStamper() {}
  public static final Stamper instance = new ContentHashStamper();
  
  /**
   * @see org.sugarj.common.cleardep.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ContentHashStamp(0);
    
    try {
      return new ContentHashStamp(FileCommands.fileHash(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ContentHashStamp(-1);
    }
  }

  
  public static class ContentHashStamp extends SimpleStamp<Integer> {

    private static final long serialVersionUID = 7535020621495360152L;

    public ContentHashStamp(Integer t) {
      super(t);
    }
    
    @Override
    public boolean equals(Stamp o) {
      return o instanceof ContentHashStamp && super.equals(o);
    }

    @Override
    public Stamper getStamper() {
      return ContentHashStamper.instance;
    }
  }
}
