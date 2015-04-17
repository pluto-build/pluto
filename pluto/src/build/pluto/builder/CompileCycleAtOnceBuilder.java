package build.pluto.builder;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.common.path.Path;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.builder.BuildCycle.Result;
import build.pluto.dependency.BuildRequirement;
import build.pluto.stamp.LastModifiedStamper;

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

  
  protected <
  In_ extends Serializable, 
  Out_ extends Serializable, 
  B_ extends Builder<ArrayList<In_>, Out_>,
  F_ extends BuilderFactory<ArrayList<In_>, Out_, B_>
  > Out_ requireCyclicable(F_ factory, In_ input) throws IOException {
    BuildRequest<ArrayList<In_>, Out_, B_, F_> req = new BuildRequest<ArrayList<In_>, Out_, B_, F_>(factory, CompileCycleAtOnceBuilder.singletonArrayList(input));
    BuildUnit<Out_> e = manager.require(req);
    result.requires(e);
    return e.getBuildResult();
  }
  
  @Override
  public void provide(Path p) {throw new AssertionError();};
  
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
      if (req.getRequest().factory != this.factory) {
        System.out.println("Not the same factory");
        return false;
      }
      if (!(req.getRequest().input instanceof ArrayList<?>)) {
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
       cyclicResults.add((BuildUnit<Out>) req.getUnit());
       inputs.addAll((ArrayList<In>) req.getRequest().input);
     }
     
     CompileCycleAtOnceBuilder<In, Out > newBuilder = factory.makeBuilder(inputs);
     newBuilder.manager = manager;
     for (BuildUnit<Out> unit : cyclicResults) {
       BuildUnit.create(unit.getPersistentPath(), unit.getGeneratedBy());
     }
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
       inputs.addAll((ArrayList<In>) req.getRequest().input);
     }
    CompileCycleAtOnceBuilder<In, Out > newBuilder = factory.makeBuilder(inputs);
    return newBuilder.description();
  }
  

}
