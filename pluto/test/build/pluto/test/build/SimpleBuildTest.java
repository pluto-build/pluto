package build.pluto.test.build;

import java.io.IOException;

import build.pluto.builder.BuildRequest;
import build.pluto.test.build.SimpleBuilder.TestBuilderInput;

public abstract class SimpleBuildTest extends ScopedBuildTest{
	
	@Override
	protected String getTestFolderName() {
		return this.getClass().getSimpleName();
	}

	protected TrackingBuildManager buildMainFile() throws IOException {
		TrackingBuildManager manager = new TrackingBuildManager();
		buildMainFile(manager);
		return manager;
	}

	protected void buildMainFile(TrackingBuildManager manager) throws IOException {
		System.out.println("====== Build Project .... ======");
		BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath, getRelativeFile("main.txt")));
		manager.require(null, req);
	}
	
	protected abstract BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input);
	
	
}
