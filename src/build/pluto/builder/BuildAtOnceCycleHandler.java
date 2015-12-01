package build.pluto.builder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;

public class BuildAtOnceCycleHandler
<
  In extends Serializable,
  Out extends Output,
  B extends BuildCycleAtOnceBuilder<In, Out>,
  F extends BuilderFactory<ArrayList<In>, Out, B>
>
//@formatter:on
extends CycleHandler {

  private final F builderFactory;

  protected BuildAtOnceCycleHandler(BuildCycle cycle, F builderFactory) {
    super(cycle);
    this.builderFactory = builderFactory;
  }

  @Override
  public boolean canBuildCycle(BuildCycle cycle) {
    for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents())
      if (req.factory != builderFactory || !(req.input instanceof ArrayList<?>))
        return false;
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<BuildUnit<?>> buildCycle(BuildCycle cycle, BuildUnitProvider manager) throws Throwable {
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

    List<Out> outputs = newBuilder.buildAll(inputs);
    if (outputs.size() != inputs.size()) {
      throw new AssertionError("buildAll needs to return one output for one input, but was " + outputs);
    }

    for (int i = 0; i < outputs.size(); i++) {
      BuildUnit<Out> unit = cyclicResults.get(i);
      unit.setBuildResult(outputs.get(i));
      unit.setState(State.finished(true));
    }
    /*
     * for (BuildUnit<Out> out1 : cyclicResults) { for (BuildUnit<Out> out2 :
     * cyclicResults) { if (out1 != out2) out1.requires(new
     * BuildRequirement<>(out2, out2.getGeneratedBy())); } }
     */
    for (int i = 0; i < cyclicResults.size(); i++) {
      BuildUnit<Out> unit1 = cyclicResults.get(i);
      BuildUnit<Out> unit2 = cyclicResults.get((i + 1 == cyclicResults.size()) ? 0 : i);
      unit1.requires(new BuildRequirement<>(unit2, unit2.getGeneratedBy()));
    }

    return new HashSet<BuildUnit<?>>(cyclicResults);
  }

  @SuppressWarnings("unchecked")
  @Override
  public String cycleDescription(BuildCycle cycle) {
    ArrayList<In> inputs = new ArrayList<>(cycle.getCycleComponents().size());
    for (BuildRequest<?, ?, ?, ?> request : cycle.getCycleComponents()) {
      // Cast is safe, otherwise cycle handler rejected to compile the cycle
      inputs.addAll((ArrayList<In>) request.input);
    }
    return builderFactory.makeBuilder(inputs).description();
  }

}
