package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;

public abstract class CompileCycleAtOnceBuilder<In extends Serializable, Out extends Output> extends Builder<ArrayList<In>, Out> implements CycleSupport {

  public static <X> ArrayList<X> singletonArrayList(X elem) {
    return new ArrayList<X>(Collections.<X> singletonList(elem));
  }

  private final BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In, Out>> factory;

  public CompileCycleAtOnceBuilder(In input, BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In, Out>> factory) {
    this(singletonArrayList(input), factory);
  }

  public CompileCycleAtOnceBuilder(ArrayList<In> input, BuilderFactory<ArrayList<In>, Out, ? extends CompileCycleAtOnceBuilder<In, Out>> factory) {
    super(input);
    this.factory = factory;
  }

  @Override
  protected CycleSupport getCycleSupport() {
    return this;
  }

  protected abstract File singletonPersistencePath(In input);

  private List<BuildUnit<Out>> cyclicResults;

  @Override
  public void require(File p) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.requires(p, defaultStamper().stampOf(p));
    }
  }

  protected <In_ extends Serializable, Out_ extends Output, B_ extends Builder<ArrayList<In_>, Out_>, F_ extends BuilderFactory<ArrayList<In_>, Out_, B_>> Out_ requireCyclicable(F_ factory, In_ input) throws IOException {
    BuildRequest<ArrayList<In_>, Out_, B_, F_> req = new BuildRequest<ArrayList<In_>, Out_, B_, F_>(factory, CompileCycleAtOnceBuilder.singletonArrayList(input));
    BuildRequirement<Out_> e = manager.require(req);
    result.requires(e);
    return e.getUnit().getBuildResult();
  }

  @Override
  public void provide(File p) {
    throw new AssertionError();
  };

  public void generates(In input, File p) {
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
  protected File persistentPath() {
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
    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      if (req.factory != this.factory) {
        System.out.println("Not the same factory");
        return false;
      }
      if (!(req.input instanceof ArrayList<?>)) {
        System.out.println("No array list input");
        return false;
      }
    }
    return true;
  }

  @Override
  public BuildCycleResult compileCycle(BuildUnitProvider manager, BuildCycle cycle) throws Throwable {
    ArrayList<BuildUnit<Out>> cyclicResults = new ArrayList<>();
    ArrayList<In> inputs = new ArrayList<>();
    ArrayList<BuildRequest<?, Out, ?, ?>> requests = new ArrayList<>();

    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      Builder<?, ?> tmpBuilder = req.createBuilder();
      cyclicResults.add(BuildUnit.<Out> create(tmpBuilder.persistentPath(), (BuildRequest<?, Out, ?, ?>) req));
      inputs.addAll((ArrayList<In>) req.input);
      requests.add((BuildRequest<?, Out, ?, ?>) req);
    }

    CompileCycleAtOnceBuilder<In, Out> newBuilder = factory.makeBuilder(inputs);
    newBuilder.manager = manager;
    newBuilder.cyclicResults = cyclicResults;

    List<Out> outputs = newBuilder.buildAll();
    if (outputs.size() != inputs.size()) {
      throw new AssertionError("buildAll needs to return one output for one input");
    }

    BuildCycleResult result = new BuildCycleResult();
    for (int i = 0; i < outputs.size(); i++) {
      result.setBuildResult(requests.get(i), outputs.get(i));
    }
    return result;
  }

  @Override
  public String getCycleDescription(BuildCycle cycle) {
    ArrayList<In> inputs = new ArrayList<>();

    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      inputs.addAll((ArrayList<In>) req.input);
    }
    CompileCycleAtOnceBuilder<In, Out> newBuilder = factory.makeBuilder(inputs);
    return newBuilder.description();
  }

}
