package org.sugarj.common.cleardep;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 *
 */
public class TimeStamper implements Stamper {

  private TimeStamper() {}
  public static final Stamper instance = new TimeStamper();
  
  /**
   * @see org.sugarj.common.cleardep.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public int stampOf(Path p) {
    if (!FileCommands.exists(p))
      return 0;
    
    return (int) (p.getFile().lastModified() % Integer.MAX_VALUE);
  }

}
