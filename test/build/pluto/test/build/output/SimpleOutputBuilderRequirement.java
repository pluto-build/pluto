package build.pluto.test.build.output;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleOutputBuilderRequirement extends BuildRequest<TestBuilderInput, Output, SimpleOutputBuilder, BuilderFactory<TestBuilderInput, Output, SimpleOutputBuilder>> {

	public SimpleOutputBuilderRequirement(
	    BuilderFactory<TestBuilderInput, Output, SimpleOutputBuilder> factory,
			TestBuilderInput input) {
		super(factory, input);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2323402524730617911L;
	

}
