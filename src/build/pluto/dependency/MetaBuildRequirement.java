package build.pluto.dependency;

import java.io.IOException;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.output.Output;

public class MetaBuildRequirement<Out extends Output> extends BuildRequirement<Out> {
  public MetaBuildRequirement() {

  }

  public MetaBuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    super(unit, req);
  }

  @Override
  public boolean tryMakeConsistent(BuildUnitProvider manager) throws IOException {
    // try to set metaBuilding
    IMetaBuildingEnabled meta = (IMetaBuildingEnabled) this.getRequest().input;
    if (meta != null) {
      meta.setMetaBuilding(true);
    } else {
      throw new IOException("Could not enable Metabuilding...");
    }

    manager.require(this.getRequest(), false);

    return true;

  }
}
