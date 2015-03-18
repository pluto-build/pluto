package org.sugarj.cleardep.dependency;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.build.BuildUnitProvider;
import org.sugarj.cleardep.output.OutputStamp;
import org.sugarj.cleardep.output.OutputStamper;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildOutputRequirement<Out extends Serializable> implements Requirement, Externalizable {
  private static final long serialVersionUID = 6148973732378610648L;

  public BuildUnit<Out> unit;
  public OutputStamp<? super Out> stamp;

  public BuildOutputRequirement() {

  }

  public BuildOutputRequirement(BuildUnit<Out> unit, OutputStamper<? super Out> stamper) {
    Objects.requireNonNull(unit);
    Objects.requireNonNull(stamper);
    this.unit = unit;
    this.stamp = stamper.stampOf(unit.getBuildResult());
  }

  public boolean isConsistent() {
    if (unit == null)
      return false;
    return stamp.equals(stamp.getStamper().stampOf(this.unit.getBuildResult()));
  }
  @Override
  public boolean isConsistentInBuild(BuildUnit<?> parent, BuildUnitProvider manager) {
    return isConsistent();
  }

  @Override
  public String toString() {
    return stamp.toString();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(unit.getPersistentPath());
    out.writeObject(stamp);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    Path unitPath = (Path) in.readObject();
    unit = BuildUnit.read(unitPath);
    stamp = (OutputStamp<? super Out>) in.readObject();
  }
  
}
