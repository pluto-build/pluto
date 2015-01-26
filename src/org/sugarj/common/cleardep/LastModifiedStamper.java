package org.sugarj.common.cleardep;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 *
 */
public class LastModifiedStamper implements Stamper {

  private LastModifiedStamper() {}
  public static final Stamper instance = new LastModifiedStamper();
  
  /**
   * @see org.sugarj.common.cleardep.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new LastModifiedStamp(0l);
    
    return new LastModifiedStamp(p.getFile().lastModified());
  }

  
  public static class LastModifiedStamp extends SimpleStamp<Long> {
    private static final long serialVersionUID = 4063932604040295576L;

    public LastModifiedStamp(Long t) {
      super(t);
    }
    
    @Override
    public boolean equals(Stamp o) {
      return o instanceof LastModifiedStamp && super.equals(o);
    }

    @Override
    public Stamper getStamper() {
      return LastModifiedStamper.instance;
    }
  }
}
