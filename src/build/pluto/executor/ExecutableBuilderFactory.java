package build.pluto.executor;

import java.io.Serializable;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.Output;

public interface ExecutableBuilderFactory<In extends Serializable, Out extends Output, B extends Builder<In, Out>> 
	extends BuilderFactory<In, Out, B> {

	public InputParser<In> inputParser();
}
