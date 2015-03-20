package org.sugarj.cleardep.dependency;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.build.BuildUnitProvider;
import org.sugarj.cleardep.build.IMetaBuildingEnabled;

public class MetaBuildRequirement<Out extends Serializable> extends BuildRequirement<Out> {
  public MetaBuildRequirement() {

  }
  
  public MetaBuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    super(unit, req);
  }
  
  @Override
  public boolean isConsistentInBuild(BuildUnit<?> parent, BuildUnitProvider manager) throws IOException{
    // try to set metaBuilding
    IMetaBuildingEnabled meta = (IMetaBuildingEnabled)this.req.input;
    if (meta != null)
    {
      meta.setMetaBuilding(true);
    } else {
      throw new IOException("Could not enable Metabuilding...");
    }
    
    manager.require(parent, this.req);
    
   return true;
   
  }
}
