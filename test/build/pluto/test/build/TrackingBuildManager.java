package build.pluto.test.build;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sugarj.common.Log;

import build.pluto.builder.BuildCycleException;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;
import build.pluto.util.IReporting.BuildReason;
import build.pluto.util.LogReporting;

public class TrackingBuildManager extends BuildManager {

	private List<Serializable> requiredInputs = new ArrayList<>();
	private List<Serializable> executedInputs = new ArrayList<>();
	private List<Serializable> successfullyExecutedInputs = new ArrayList<>();

	public TrackingBuildManager() {
		super(new LogReporting());
	}

	public <In extends Serializable, Out extends Output, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildRequirement<Out> 
	    require(F factory, In input) throws IOException {
    return require(factory, input, true);
  }

	public <In extends Serializable, Out extends Output, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildRequirement<Out> 
	    require(F factory, In input, boolean needBuildResult) throws IOException {
		requiredInputs.add(input);
		return super.require(new BuildRequest<>(factory, input), needBuildResult);
	}

	@Override
	public <In extends Serializable, Out extends Output, B extends Builder<In, Out>, F extends BuilderFactory<In, Out, B>> BuildRequirement<Out> 
	    require(BuildRequest<In, Out, B, F> buildReq, boolean needBuildResult) throws IOException {
		requiredInputs.add(buildReq.input);
		Log.log.log("Require " + buildReq, Log.DETAIL);
		return super.require(buildReq, needBuildResult);
	}

	 // @formatter:off
	@Override
  protected 
    <In extends Serializable,
     Out extends Output,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
	BuildRequirement<Out> executeBuilder(Builder<In, Out> builder, File dep, BuildRequest<In, Out, B, F> buildReq, Set<BuildReason> reasons) throws IOException {
		executedInputs.add(buildReq.input);
		try {
		  BuildRequirement<Out> result = super.executeBuilder(builder, dep, buildReq, reasons);
  		successfullyExecutedInputs.add(buildReq.input);
  		return result;
		} catch (BuildCycleException e) {
		//  if (e.getCycleState() == CycleState.RESOLVED)
		 //   successfullyExecutedInputs.add(buildReq.input);
		  throw e;
		}
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
