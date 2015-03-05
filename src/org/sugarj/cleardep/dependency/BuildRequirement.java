package org.sugarj.cleardep.dependency;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.common.path.Path;

public class BuildRequirement<Out extends BuildOutput> implements Requirement, Externalizable {
  private static final long serialVersionUID = 6148973732378610648L;

  public BuildUnit<Out> unit;
  public BuildRequest<?, Out, ?, ?> req;
  
  public BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    this.unit = unit;
    this.req = req;
  }
  
  @Override
  public boolean isConsistent() {
    return unit == null || unit.getGeneratedBy().deepEquals(req);
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
    unit = BuildUnit.read(unitPath, req);
  }
}