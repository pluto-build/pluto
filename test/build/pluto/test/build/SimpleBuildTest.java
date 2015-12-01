package build.pluto.test.build;

import java.io.File;
import java.io.IOException;

import build.pluto.builder.BuildRequest;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public abstract class SimpleBuildTest extends ScopedBuildTest{
	

	protected TrackingBuildManager buildMainFile() throws IOException {
		return buildMainFile(new TrackingBuildManager());
	}

	protected TrackingBuildManager buildMainFile(TrackingBuildManager manager) throws IOException {
		return buildFile(getRelativeFile("main.txt"), manager);
	}
	
	protected final TrackingBuildManager buildFile(File path) throws IOException {
		return buildFile(path, new TrackingBuildManager());
	}
	
	protected final TrackingBuildManager buildFile(File path, TrackingBuildManager manager) throws IOException {
		System.out.println("====== Build " + path.getPath()+" ======");
		BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath.toFile(), path));
		manager.require(req, false);
		return manager;
	}
	
	protected abstract BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input);
	
	
}
