package build.pluto.test.build.once;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.None;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleRequirement extends BuildRequest<TestBuilderInput, None, SimpleBuilder, BuilderFactory<TestBuilderInput, None, SimpleBuilder>> {

	public SimpleRequirement(
			BuilderFactory<TestBuilderInput, None, SimpleBuilder> factory,
			TestBuilderInput input) {
		super(factory, input);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2323402524730617911L;
	

}
