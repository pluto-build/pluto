package build.pluto.test.build;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.path.Path;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;

public class TrackingBuildManager extends BuildManager {

	private List<Serializable> requiredInputs = new ArrayList<>();
	private List<Serializable> executedInputs = new ArrayList<>();
	private List<Serializable> successfullyExecutedInputs = new ArrayList<>();

	public TrackingBuildManager() {
		super(null);
	}

	public <In extends Serializable, Out extends Serializable, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildUnit<Out> require(
			F factory, In input) throws IOException {
		requiredInputs.add(input);
		return super.require(new BuildRequest<>(factory, input));
	}

	@Override
	public <In extends Serializable, Out extends Serializable, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildUnit<Out> require(
			BuildRequest<In, Out, B, F> buildReq) throws IOException {
		requiredInputs.add(buildReq.input);
		return super.require(buildReq);
	}

	 // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildUnit<Out> executeBuilder(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {
		executedInputs.add(buildReq.input);
		BuildUnit<Out> result = super.executeBuilder(builder, dep, buildReq);
		successfullyExecutedInputs.add(buildReq.input);
		return result;
	}

	public List<Serializable> getRequiredInputs() {
		return requiredInputs;
	}

	public List<Serializable> getExecutedInputs() {
		return executedInputs;
	}

	public List<Serializable> getSuccessfullyExecutedInputs() {
		return successfullyExecutedInputs;
	}

}
