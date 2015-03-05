package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.BuildUnit.State;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

public abstract class Builder<In extends Serializable, Out extends BuildOutput> {
  protected final In input;
  
  public Builder(In input) {
    this.input = input;
  }
  
  public In getInput() {
    return input;
  }
  
  
  /**
   * Provides the task description for the builder and its input.
   * The description is used for console logging when the builder is run.
   * 
   * @return the task description or `null` if no logging is wanted.
   */
  protected abstract String taskDescription();
  protected abstract Path persistentPath();
  protected abstract Stamper defaultStamper();
  protected abstract Out build() throws Throwable;

  protected CycleSupport getCycleSupport() {
    return null;
  }

  transient BuildUnit<Out> result;
  private transient BuildManager manager;
  private transient Stamper defaultStamper;
  Out triggerBuild(BuildUnit<Out> result, BuildManager manager) throws Throwable {
    this.result = result;
    this.manager = manager;
    this.defaultStamper = defaultStamper();
    try {
      return build();
    } finally {
      this.result = null;
      this.manager = null;
      this.defaultStamper = null;
    }
  }
  
  protected <
  In_ extends Serializable, 
  Out_ extends BuildOutput, 
  B_ extends Builder<In_,Out_>,
  F_ extends BuilderFactory<In_, Out_, B_>,
  SubIn_ extends In_
  > Out_ require(F_ factory, SubIn_ input) throws IOException {
    BuildRequest<In_, Out_, B_, F_> req = new BuildRequest<In_, Out_, B_, F_>(factory, input);
    BuildUnit<Out_> e = manager.require(req);
    result.requires(e);
    return e.getBuildResult();
  }
  
  protected <
  In_ extends Serializable, 
  Out_ extends BuildOutput, 
  B_ extends Builder<In_,Out_>,
  F_ extends BuilderFactory<In_, Out_, B_>
  > Out_ require(BuildRequest<In_, Out_, B_, F_> req) throws IOException {
    BuildUnit<Out_> e = manager.require(req);
    result.requires(e);
    return e.getBuildResult();
  }
  
  protected <
  In_ extends Serializable, 
  Out_ extends BuildOutput, 
  B_ extends Builder<ArrayList<In_>, Out_>,
  F_ extends BuilderFactory<ArrayList<In_>, Out_, B_>
  > Out_ requireCyclicable(F_ factory, In_ input) throws IOException {
    BuildRequest<ArrayList<In_>, Out_, B_, F_> req = new BuildRequest<ArrayList<In_>, Out_, B_, F_>(factory, CompileCycleAtOnceBuilder.singletonArrayList(input));
    BuildUnit<Out_> e = manager.require(req);
    result.requires(e);
    return e.getBuildResult();
  }

    
  protected void require(BuildRequest<?, ?, ?, ?>[] reqs) throws IOException {
    if (reqs != null)
      for (BuildRequest<?, ?, ?, ?> req : reqs)
        require(req);
  }
  
  public void requires(Path p) {
    result.requires(p, defaultStamper.stampOf(p));
  }
  public void requires(Path p, Stamper stamper) {
    result.requires(p, stamper.stampOf(p));
  }
  public void requires(Path p, Stamp stamp) {
    result.requires(p, stamp);
  }
  
  public void generates(Path p) {
    result.generates(p, LastModifiedStamper.instance.stampOf(p));
  }
  public void generates(Path p, Stamper stamper) {
    result.generates(p, stamper.stampOf(p));
  }
  
  public void setState(State state) {
    result.setState(state);
  }

  public BuildUnit<Out> getBuildUnit() {
    return result;
  }
}
