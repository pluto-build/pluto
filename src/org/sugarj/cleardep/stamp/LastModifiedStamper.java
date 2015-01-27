package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 *
 */
public class LastModifiedStamper implements Stamper, ModuleStamper {

  private LastModifiedStamper() {}
  public static final Stamper instance = new LastModifiedStamper();
  public static final ModuleStamper minstance = new LastModifiedStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public LastModifiedStamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new LastModifiedStamp(0l);
    
    return new LastModifiedStamp(p.getFile().lastModified());
  }
  
  public LastModifiedStamp stampOf(CompilationUnit m) {
    if (!m.isPersisted())
      throw new IllegalArgumentException("Cannot compute stamp of non-persisted compilation unit " + m);

    return stampOf(m.getPersistentPath());
  }

  
  public static class LastModifiedStamp implements Stamp, ModuleStamp {
    private static final long serialVersionUID = 4063932604040295576L;

    private final Long value;
    
    public LastModifiedStamp(Long value) {
      this.value = value;
    }
    
    @Override
    public boolean equals(Stamp o) {
      if (!(o instanceof LastModifiedStamp))
        return false;
      Long ovalue = ((LastModifiedStamp) o).value;
      return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
    }
    
    @Override
    public boolean equals(ModuleStamp o) {
      if (!(o instanceof LastModifiedStamp))
        return false;
      Long ovalue = ((LastModifiedStamp) o).value;
      return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
    }

    @Override
    public Stamper getStamper() {
      return LastModifiedStamper.instance;
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return LastModifiedStamper.minstance;
    }
  }
}
