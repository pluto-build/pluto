package build.pluto.dependency;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.builder.IMetaBuildingEnabled;

public class MetaBuildRequirement<Out extends Serializable> extends BuildRequirement<Out> {
  public MetaBuildRequirement() {

  }
  
  public MetaBuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    super(unit, req);
  }
  
  @Override
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException{
    // try to set metaBuilding
    IMetaBuildingEnabled meta = (IMetaBuildingEnabled)this.req.input;
    if (meta != null)
    {
      meta.setMetaBuilding(true);
    } else {
      throw new IOException("Could not enable Metabuilding...");
    }
    
    manager.require(this.req);
    
   return true;
   
  }
}
