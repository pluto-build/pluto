package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;
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
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new LastModifiedStamp(0l);
    
    return new LastModifiedStamp(p.getFile().lastModified());
  }
  
  public Stamp stampOf(CompilationUnit m) {
    if (!m.isPersisted())
      throw new IllegalArgumentException("Cannot compute stamp of non-persisted compilation unit " + m);

    return stampOf(m.getPersistentPath());
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
