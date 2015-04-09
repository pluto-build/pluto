package build.pluto.test.build;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.None;
import build.pluto.test.build.SimpleBuilder.TestBuilderInput;

public class TestRequirement extends BuildRequest<TestBuilderInput, None, SimpleBuilder, BuilderFactory<TestBuilderInput, None, SimpleBuilder>> {

	public TestRequirement(
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
