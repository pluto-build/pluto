package build.pluto.builder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.output.Output;

public class CompileAtOnceCycleSupport
//@formatter:off
<
  In extends Serializable,
  Out extends Output,
  B extends CompileCycleAtOnceBuilder<In, Out>,
  F extends BuilderFactory<ArrayList<In>, Out, B>
>
//@formatter:on
implements CycleSupport {

  private final BuildCycle cycle;
  private final F builderFactory;

  protected CompileAtOnceCycleSupport(BuildCycle cycle, F builderFactory) {
    super();
    this.cycle = cycle;
    this.builderFactory = builderFactory;
  }

  @Override
  public boolean canCompileCycle() {
    return cycle.getCycleComponents().stream().allMatch((BuildRequest<?, ?, ?, ?> req) -> req.factory == builderFactory && (req.input instanceof ArrayList<?>));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<BuildUnit<?>> compileCycle(BuildUnitProvider manager) throws Throwable {
    ArrayList<BuildUnit<Out>> cyclicResults = new ArrayList<>();
    ArrayList<In> inputs = new ArrayList<>();
    ArrayList<BuildRequest<?, Out, ?, ?>> requests = new ArrayList<>();

    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents()) {
      Builder<?, ?> tmpBuilder = req.createBuilder();
      // Casts are safe, otherwise the cycle support would had rejected to
      // compile the cycle
      cyclicResults.add(BuildUnit.<Out> create(tmpBuilder.persistentPath(), (BuildRequest<?, Out, ?, ?>) req));
      inputs.addAll((ArrayList<In>) req.input);
      requests.add((BuildRequest<?, Out, ?, ?>) req);
    }

    B newBuilder = builderFactory.makeBuilder(inputs);
    newBuilder.manager = manager;
    newBuilder.cyclicResults = cyclicResults;

    List<Out> outputs = newBuilder.buildAll();
    if (outputs.size() != inputs.size()) {
      throw new AssertionError("buildAll needs to return one output for one input");
    }

    for (int i = 0; i < outputs.size(); i++) {
      BuildUnit<Out> unit = cyclicResults.get(i);
      unit.setBuildResult(outputs.get(i));
      unit.setState(State.finished(true));
    }
    return new HashSet<>(cyclicResults);
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getCycleDescription() {
    ArrayList<In> inputs = new ArrayList<>(cycle.getCycleComponents().size());
    for (BuildRequest<?, ?, ?, ?> request : cycle.getCycleComponents()) {
      // Cast is safe, otherwise cycle handler rejected to compile the cycle
      inputs.addAll((ArrayList<In>) request.input);
    }
    return builderFactory.makeBuilder(inputs).description();
  }

}
