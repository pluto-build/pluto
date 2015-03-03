package org.sugarj.cleardep.build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.BuildUnit.State;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.output.BuildOutput;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.common.path.Path;
import org.sugarj.common.util.Pair;

public abstract class CompileCycleAtOnceBuilder<T extends Serializable, Out extends BuildOutput> extends Builder<ArrayList<T>, Out> {

  public static <X> ArrayList<X> singletonArrayList(X elem) {
    return new ArrayList<X>(Collections.<X> singletonList(elem));
  }

  private final BuilderFactory<ArrayList<T>, Out, ? extends CompileCycleAtOnceBuilder<T,Out>> factory;
  
  public CompileCycleAtOnceBuilder(T input, BuilderFactory<ArrayList<T>, Out, ? extends CompileCycleAtOnceBuilder<T,Out>> factory) {
    this(singletonArrayList(input), factory);
  }

  public CompileCycleAtOnceBuilder(ArrayList<T> input,BuilderFactory<ArrayList<T>, Out, ? extends CompileCycleAtOnceBuilder<T,Out>> factory) {
    super(input);
    this.factory = factory;
  }

  protected abstract Path singletonPersistencePath(T input);

  private List<BuildUnit<Out>> cyclicResults;

  @Override
  protected String cyclicTaskDescription(List<BuildRequirement<?>> cycle) {
    return this.taskDescription();
  }
  
  @Override
  public void requires(Path p) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.requires(p, defaultStamper().stampOf(p));
    }
  }

  @Override
  public void generates(Path p) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.generates(p, LastModifiedStamper.instance.stampOf(p));
     
    }
  }
  
  @Override
  public void setState(State state) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.setState(state);
    }
  }

  @Override
  protected Path persistentPath() {
    if (this.input.size() == 1) {
      return this.singletonPersistencePath(this.input.get(0));
    } else {
      throw new AssertionError("Should not occur");
    }
  }

  @Override
  protected boolean canBuildCycle(List<BuildRequirement<?>> cycle) {
    for (BuildRequirement<?> req : cycle) {
      if (req.req.factory != this.factory) {
        System.out.println("Not the same factory");
        return false;
      }
      if (!(req.req.input instanceof ArrayList<?>)) {
        System.out.println("No array list input");
        return false;
      }
    }
    return true;
  }

  @Override
  protected void buildCycle(List<BuildRequirement<?>> cycle) throws Throwable {
   ArrayList<BuildUnit<Out>> cyclicResults = new ArrayList<>();
   ArrayList<T> inputs = new ArrayList<>();
   
    for (BuildRequirement<?> req : cycle) {
      cyclicResults.add((BuildUnit<Out>) req.unit);
      inputs.addAll((ArrayList<T>) req.req.input);
    }
    
    CompileCycleAtOnceBuilder<T, Out > newBuilder = factory.makeBuilder(inputs);
    for (BuildUnit<Out> result : cyclicResults) {
      result = BuildUnit.create(result.getPersistentPath(), result.getGeneratedBy());
    }
    newBuilder.cyclicResults = cyclicResults;
    newBuilder.buildInternal();
  }
  
  @Override
  protected Out build() throws Throwable {
    
   this.cyclicResults = singletonArrayList(super.result);
   return buildInternal();
  }
  
protected Out buildInternal() throws Throwable {
  if ( cyclicResults == null) {
    throw new AssertionError("There should be a result");
  }
    Out out = buildCycle();
    for (BuildUnit<Out> result : cyclicResults) {
      result.setBuildResult(out);
    }
    return out;
  }

  protected abstract Out buildCycle() throws Throwable;

}
