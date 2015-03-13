package org.sugarj.cleardep.build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.BuildUnit.State;
import org.sugarj.cleardep.build.BuildCycle.Result;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.common.path.Path;

public abstract class CompileCycleAtOnceBuilder<In extends Serializable, Out extends Serializable> extends Builder<ArrayList<In>, Out> implements CycleSupport{

  public static <X> ArrayList<X> singletonArrayList(X elem) {
    return new ArrayList<X>(Collections.<X> singletonList(elem));
  }

  private final BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In,Out>> factory;
  
  public CompileCycleAtOnceBuilder(In input, BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In,Out>> factory) {
    this(singletonArrayList(input), factory);
  }

  public CompileCycleAtOnceBuilder(ArrayList<In> input,BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In,Out>> factory) {
    super(input);
    this.factory = factory;
  }
  
  @Override
  protected CycleSupport getCycleSupport() {
    return this;
  }

  protected abstract Path singletonPersistencePath(In input);

  private List<BuildUnit<Out>> cyclicResults;
  
  @Override
  public void require(Path p) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.requires(p, defaultStamper().stampOf(p));
    }
  }

  @Override
  public void generate(Path p) {throw new AssertionError();};
  
  public void generates(In input, Path p) {
    for (int i = 0; i < this.input.size(); i++) {
      if (this.input.get(i) == input) {
      this.cyclicResults.get(i).generates(p, LastModifiedStamper.instance.stampOf(p));
      }
     
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
  protected Out build() throws Throwable {
    this.cyclicResults = Collections.singletonList(super.result);
    List<Out> result = this.buildAll();
    if (result.size() != 1) {
      throw new AssertionError("buildAll needs to return one output for one input");
    }
    return result.get(0);
  }
  
  protected abstract List<Out> buildAll() throws Throwable; 

  @Override
  public boolean canCompileCycle(BuildCycle cycle) {
    for (BuildRequirement<?> req : cycle.getCycleComponents()) {
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
  public Result compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable {
    ArrayList<BuildUnit<Out>> cyclicResults = new ArrayList<>();
    ArrayList<In> inputs = new ArrayList<>();
    
     for (BuildRequirement<?> req : cycle.getCycleComponents()) {
       cyclicResults.add((BuildUnit<Out>) req.unit);
       inputs.addAll((ArrayList<In>) req.req.input);
     }
     
     CompileCycleAtOnceBuilder<In, Out > newBuilder = factory.makeBuilder(inputs);
     newBuilder.cyclicResults = cyclicResults;
     
     List<Out> outputs = newBuilder.buildAll();
     if (outputs.size() != inputs.size()) {
       throw new AssertionError("buildAll needs to return one output for one input");
     }
     
     Result result = new Result();
     for (int i = 0;  i < outputs.size(); i++) {
       result.setBuildResult(cyclicResults.get(i), outputs.get(i));
     }
     return result;
  }
  
  @Override
  public String getCycleDescription(BuildCycle cycle) {
    ArrayList<In> inputs = new ArrayList<>();
    
     for (BuildRequirement<?> req : cycle.getCycleComponents()) {
       inputs.addAll((ArrayList<In>) req.req.input);
     }
    CompileCycleAtOnceBuilder<In, Out > newBuilder = factory.makeBuilder(inputs);
    return newBuilder.taskDescription();
  }
  

}
