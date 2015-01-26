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
  public int stampOf(Path p) {
    if (!FileCommands.exists(p))
      return 0;
    
    try {
      return FileCommands.fileHash(p);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }

}
