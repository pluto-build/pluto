package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

public abstract class Builder<T extends Serializable, E extends BuildUnit> {
  protected final BuildManager manager;
  protected final BuilderFactory<T, E, ? extends Builder<T,E>> sourceFactory;
  protected final T input;
  
  public Builder(T input, BuilderFactory<T, E, ? extends Builder<T, E>> sourceFactory, BuildManager manager) {
    Objects.requireNonNull(sourceFactory);
    Objects.requireNonNull(manager);
    this.input = input;
    this.sourceFactory = sourceFactory;
    this.manager = manager;
  }
  
  public T getInput() {
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
  protected abstract Class<E> resultClass();
  protected abstract Stamper defaultStamper();
  protected abstract void build(E result) throws Throwable;
  
  private E result;
  void triggerBuild(E result) throws Throwable {
    this.result = result;
    try {
      build(result);
    } finally {
      this.result = null;
    }
  }
  
  protected <
  T_ extends Serializable, 
  E_ extends BuildUnit, 
  B_ extends Builder<T_,E_>,
  F_ extends BuilderFactory<T_, E_, B_>,
  SubT_ extends T_
  > E_ require(F_ factory, SubT_ input) throws IOException {
    BuildRequest<T_, E_, B_, F_> req = new BuildRequest<T_, E_, B_, F_>(factory, input);
    E_ e = manager.require(req);
    result.requires(e);
    return e;
  }
  
  protected <
  T_ extends Serializable, 
  E_ extends BuildUnit,
  B_ extends Builder<T_,E_>, 
  F_ extends BuilderFactory<T_, E_, B_>> E_ require(BuildRequest<T_, E_, B_, F_> req) throws IOException {
    E_ e = manager.require(req);
    result.requires(e);
    return e;
  }
  
  protected void require(BuildRequest<?, ?, ?, ?>[] reqs) throws IOException {
    if (reqs != null)
      for (BuildRequest<?, ?, ?, ?> req : reqs)
        require(req);
  }
}
