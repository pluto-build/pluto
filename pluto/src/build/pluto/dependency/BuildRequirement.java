package build.pluto.dependency;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.common.path.Path;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildUnitProvider;

public class BuildRequirement<Out extends Serializable> implements Requirement, Externalizable {

  private static final long serialVersionUID = -5059819155907677962L;
  
  private BuildUnit<Out> unit;
  private boolean hasFailed;
  private BuildRequest<?, Out, ?, ?> req;

  public BuildRequirement() {

  }

  public BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    Objects.requireNonNull(unit);
    this.unit = unit;
    this.req = req;
  }

  @Override
  public boolean isConsistent() {
    boolean reqsEqual = unit.getGeneratedBy().deepEquals(req);
    return reqsEqual;
  }
  
  @Override
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException{
    boolean wasFailed = hasFailed || unit != null && unit.hasFailed();
    BuildUnit<Out> newUnit = manager.require(this.req);
    hasFailed = newUnit.hasFailed();

    if (wasFailed && !hasFailed)
      return false;
    
   return true;
   
  }

  @Override
  public String toString() {
    return req.toString();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(unit.getPersistentPath());
    out.writeBoolean(hasFailed);
    out.writeObject(req);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    Path unitPath = (Path) in.readObject();
    hasFailed = in.readBoolean();
    req = (BuildRequest<?, Out, ?, ?>) in.readObject();
    unit = BuildUnit.read(unitPath);
  }

  public BuildUnit<Out> getUnit() {
    return unit;
  }

  public BuildRequest<?, Out, ?, ?> getRequest() {
    return req;
  }
}
