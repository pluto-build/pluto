package org.sugarj.cleardep.stamp;

import java.util.HashMap;
import java.util.Map;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.stamp.CollectionStamper.CollectionStamp;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 *
 */
public class LastModifiedStamper implements Stamper, ModuleStamper {

  private static final long serialVersionUID = 8242859577253542194L;

  private LastModifiedStamper() {}
  public static final Stamper instance = new LastModifiedStamper();
  public static final ModuleStamper minstance = new LastModifiedStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new LastModifiedStamp(0l);
    
    if (p.getFile().isDirectory()) {
      Map<Path, Stamp> stamps = new HashMap<>();
      stamps.put(p, new LastModifiedStamp(p.getFile().lastModified()));
      
      for (Path sub : FileCommands.listFilesRecursive(p))
        stamps.put(sub, new LastModifiedStamp(sub.getFile().lastModified()));
      
      return new CollectionStamp(stamps, this);
    }
    
    return new LastModifiedStamp(p.getFile().lastModified());
  }
  
  public ModuleStamp stampOf(CompilationUnit m) {
    if (!m.isPersisted())
      throw new IllegalArgumentException("Cannot compute stamp of non-persisted compilation unit " + m);

    if (!FileCommands.exists(m.getPersistentPath()))
      return new LastModifiedStamp(0l);
    
    return new LastModifiedStamp(m.getPersistentPath().getFile().lastModified());
  }

  
  public static class LastModifiedStamp implements Stamp, ModuleStamp {
    private static final long serialVersionUID = 4063932604040295576L;

    private final Long value;
    
    public LastModifiedStamp(Long value) {
      this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof LastModifiedStamp) {
        Long ovalue = ((LastModifiedStamp) o).value;
        return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
      }
      if (o instanceof LastModifiedStamp) {
        Long ovalue = ((LastModifiedStamp) o).value;
        return ovalue == null && value == null || ovalue != null && ovalue.equals(value);
      }
      return false;
    }
    
    public Stamper getStamper() {
      return LastModifiedStamper.instance;
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return LastModifiedStamper.minstance;
    }
    
    @Override
    public String toString() {
      return "LastModified(" + value + ")";
    }
  }
}
