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
import build.pluto.output.OutputStamp;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequirement<Out extends Serializable> implements Requirement, Externalizable {
  private static final long serialVersionUID = 6148973732378610648L;

  public BuildUnit<Out> unit;
  public BuildRequest<?, Out, ?, ?> req;

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

    manager.require(this.req);
    
   return true;
   
  }

  @Override
  public String toString() {
    return req.toString();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(unit.getPersistentPath());
    out.writeObject(req);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    Path unitPath = (Path) in.readObject();
    req = (BuildRequest<?, Out, ?, ?>) in.readObject();
    unit = BuildUnit.read(unitPath);
  }
}
