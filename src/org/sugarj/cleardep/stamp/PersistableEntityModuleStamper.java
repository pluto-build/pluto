package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;

public class PersistableEntityModuleStamper implements ModuleStamper {

  private static final long serialVersionUID = 4059061281056367108L;

  public final static PersistableEntityModuleStamper minstance = new PersistableEntityModuleStamper();
  private PersistableEntityModuleStamper() { }
  
  @Override
  public ModuleStamp stampOf(CompilationUnit m) {
    return new PersistableEntityStamp(m.stamp());
  }

  public static class PersistableEntityStamp implements ModuleStamp {

    private static final long serialVersionUID = 8933092358103291540L;

    private final Stamp stamp;
    
    public PersistableEntityStamp(Stamp stamp) {
      this.stamp = stamp;
    }
    
    @Override
    public boolean equals(ModuleStamp s) {
      return s instanceof PersistableEntityStamp && ((PersistableEntityStamp) s).stamp.equals(stamp);
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return PersistableEntityModuleStamper.minstance;
    }
  }
}
