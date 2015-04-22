package build.pluto.dependency;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuildUnitProvider;
import build.pluto.output.Output;
import build.pluto.output.OutputStamp;

public class BuildRequirement<Out extends Output> implements Requirement, Externalizable {

  private static final long serialVersionUID = -5059819155907677962L;
  
  private BuildUnit<Out> unit;
  private boolean hasFailed;
  private BuildRequest<?, Out, ?, ?> req;
  private OutputStamp<? super Out> stamp;

  public BuildRequirement() { }

  public BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req) {
    this(unit, req, req.stamper.stampOf(unit.getBuildResult()));
  }
  
  protected BuildRequirement(BuildUnit<Out> unit, BuildRequest<?, Out, ?, ?> req, OutputStamp<? super Out> stamp) {
    Objects.requireNonNull(unit);
    this.unit = unit;
    this.req = req;
    this.stamp = stamp;
  }

  @Override
  public boolean isConsistent() {
    boolean reqsEqual = unit.getGeneratedBy().deepEquals(req);
    if (!reqsEqual)
      return false;
    
    boolean stampOK = stamp == null || stamp.equals(stamp.getStamper().stampOf(this.unit.getBuildResult()));
    if (!stampOK)
      return false;
    
    return true;
  }
  
  @Override
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException{
    boolean wasFailed = hasFailed || unit != null && unit.hasFailed();
    BuildUnit<Out> newUnit = manager.require(this.req).getUnit();
    hasFailed = newUnit.hasFailed();

    if (wasFailed && !hasFailed)
      return false;
    
    boolean stampOK = stamp == null || stamp.equals(stamp.getStamper().stampOf(newUnit.getBuildResult()));
    if (!stampOK)
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
    out.writeObject(stamp);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    File unitPath = (File) in.readObject();
    hasFailed = in.readBoolean();
    req = (BuildRequest<?, Out, ?, ?>) in.readObject();
    stamp = (OutputStamp<? super Out>) in.readObject();
    unit = BuildUnit.read(unitPath);
  }

  public BuildUnit<Out> getUnit() {
    return unit;
  }

  public BuildRequest<?, Out, ?, ?> getRequest() {
    return req;
  }
}
