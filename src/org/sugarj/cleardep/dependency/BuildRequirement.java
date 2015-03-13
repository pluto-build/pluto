package org.sugarj.cleardep.dependency;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.output.OutputStamp;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequirement<Out extends Serializable> implements Requirement, Externalizable {
  private static final long serialVersionUID = 6148973732378610648L;

  public BuildUnit<Out> unit;
  public BuildRequest<?, Out, ?, ?> req;
  public OutputStamp outputStamp;

  public BuildRequirement() {

  }

  public BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    Objects.requireNonNull(unit);
    this.unit = unit;
    this.req = req;
    this.outputStamp = req.stamper.stampOf(unit.getBuildResult());
  }

  @Override
  public boolean isConsistent() {
    return unit.getGeneratedBy().deepEquals(req) && 
        outputStamp.equals(outputStamp.getStamper().stampOf(unit.getBuildResult()));
  }

  @Override
  public String toString() {
    return req.toString();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(unit.getPersistentPath());
    out.writeObject(req);
    out.writeObject(outputStamp);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    Path unitPath = (Path) in.readObject();
    req = (BuildRequest<?, Out, ?, ?>) in.readObject();
    unit = BuildUnit.read(unitPath);
    outputStamp = (OutputStamp) in.readObject();
  }
}
