package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.BuildUnit.State;
import org.sugarj.common.path.Path;
import org.sugarj.common.util.Pair;

public abstract class CompileCycleAtOnceBuilder<T extends Serializable, E extends BuildUnit> extends Builder<ArrayList<T>, E> {

  public static <X> ArrayList<X>singletonArrayList(X elem) {
    return new ArrayList<X>(Collections.<X> singletonList(elem));
  }
  
  public CompileCycleAtOnceBuilder(T input, BuilderFactory<ArrayList<T>, E, ? extends Builder<ArrayList<T>, E>> sourceFactory, BuildManager manager) {
    super(singletonArrayList(input), sourceFactory, manager);
  }

  public CompileCycleAtOnceBuilder(ArrayList<T> input, BuilderFactory<ArrayList<T>, E, ? extends Builder<ArrayList<T>, E>> sourceFactory, BuildManager manager) {
    super(input, sourceFactory, manager);
  }

  protected abstract Path singletonPersistencePath(T input);

  protected abstract Path cyclePersistencePath(List<T> input);

  @Override
  protected Path persistentPath() {
    if (this.input.size() == 1) {
      return this.singletonPersistencePath(this.input.get(0));
    } else {
      return this.cyclePersistencePath(input);
    }
  }
  
  @Override
  protected boolean canBuildCycle(List<Pair<? extends BuildUnit, BuildRequest<?, ?, ?, ?>>> cycle) {
    for (Pair<?extends BuildUnit, BuildRequest<?, ?, ?, ?>> unitPairs : cycle) {
      if (unitPairs.b.factory != this.sourceFactory) {
        System.out.println("Not the same factory");
        return false;
      }
      if (!(unitPairs.b.input instanceof ArrayList<?>)) {
        System.out.println("No array list input");
        return false;
      }
      if (!this.resultClass().equals(unitPairs.a.getClass())) {
        System.out.println("Wrong result class");
        return false;
      }
    }
    return true;
  }

  @Override
  protected void buildCycle(List<Pair<? extends BuildUnit, BuildRequest<?, ?, ?, ?>>> cycle) throws Throwable{
    ArrayList<T> inputs = new ArrayList<>(cycle.size());
    for (Pair<? extends BuildUnit, BuildRequest<?, ?, ?, ?>> unitPairs : cycle) {
      inputs.addAll((ArrayList<T>) unitPairs.b.input);
    }
    BuildRequest<ArrayList<T>, E, ? extends Builder<ArrayList<T>, E>, ?>  cycleRequirement = new BuildRequest<>(this.sourceFactory, inputs);
    manager.require(cycleRequirement);
  }
  
  @Override
  protected void build(E result) throws Throwable {
    if (this.input.size() == 1) {
      buildSingleton(result);
    } else {
      for (T cyclicInput : this.input) {
        E cyclicUnit = BuildUnit.create(resultClass(), defaultStamper(), singletonPersistencePath(cyclicInput), new BuildRequest<>(this.sourceFactory, singletonArrayList(cyclicInput)));
        cyclicUnit.dependsOn(result);
        result.dependsOn(cyclicUnit);
        cyclicUnit.setState(State.finished(true));
        cyclicUnit.write();
      }
      buildCycle(result);
    }
  }
  
  protected void buildSingleton(E result) throws Throwable{
    this.buildCycle(result);
  }
  
  protected abstract void buildCycle(E result) throws Throwable;

}
