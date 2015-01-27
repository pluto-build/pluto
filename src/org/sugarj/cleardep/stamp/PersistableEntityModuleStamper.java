package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;

public class PersistableEntityModuleStamper implements ModuleStamper {

  private static final long serialVersionUID = 4059061281056367108L;

  public final static PersistableEntityModuleStamper minstance = new PersistableEntityModuleStamper();
  private PersistableEntityModuleStamper() { }
  
  @Override
  public ModuleStamp stampOf(CompilationUnit m) {
    if (m.isPersisted())
      return new PersistableEntityModuleStamp(m.stamp());
    else
      return new PersistableEntityModuleStamp(null);
  }

  public static class PersistableEntityModuleStamp implements ModuleStamp {

    private static final long serialVersionUID = 8933092358103291540L;

    private final Stamp stamp;
    
    public PersistableEntityModuleStamp(Stamp stamp) {
      this.stamp = stamp;
    }
    
    @Override
    public boolean equals(ModuleStamp s) {
      if (!(s instanceof PersistableEntityModuleStamp))
        return false;
      Stamp ostamp = ((PersistableEntityModuleStamp) s).stamp;
      return stamp == null && ostamp == null || stamp != null && stamp.equals(ostamp);
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return PersistableEntityModuleStamper.minstance;
    }
  }
}
