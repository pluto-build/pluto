package build.pluto.builder;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;

public abstract class CompileCycleAtOnceBuilder<In extends Serializable, Out extends Output> extends Builder<ArrayList<In>, Out> {

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
  protected CycleSupportFactory getCycleSupport() {
    return (BuildCycle cycle) -> new CompileAtOnceCycleSupport<>(cycle, this.factory);
  }

  protected abstract File singletonPersistencePath(In input);

  protected List<BuildUnit<Out>> cyclicResults;

  @Override
  public void require(File p) {
    for (BuildUnit<Out> result : cyclicResults) {
      result.requires(p, defaultStamper().stampOf(p));
    }
  }

  @Override
  public void provide(File p) {
    throw new AssertionError("Cannot provide a file. Use provide(In, File) to make clear which input provides which file.");
  }

  public void provide(In input, File p) {
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



}
