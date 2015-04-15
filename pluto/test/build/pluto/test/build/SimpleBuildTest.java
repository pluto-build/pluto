package build.pluto.test.build;

import java.io.IOException;

import org.sugarj.common.path.RelativePath;

import build.pluto.builder.BuildRequest;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public abstract class SimpleBuildTest extends ScopedBuildTest{
	
	@Override
	protected String getTestFolderName() {
		return this.getClass().getSimpleName();
	}

	protected TrackingBuildManager buildMainFile() throws IOException {
		return buildMainFile(new TrackingBuildManager());
	}

	protected TrackingBuildManager buildMainFile(TrackingBuildManager manager) throws IOException {
		return buildFile(getRelativeFile("main.txt"), manager);
	}
	
	protected final TrackingBuildManager buildFile(RelativePath path) throws IOException {
		return buildFile(path, new TrackingBuildManager());
	}
	
	protected final TrackingBuildManager buildFile(RelativePath path, TrackingBuildManager manager) throws IOException {
		System.out.println("====== Build " + path.getRelativePath()+" ======");
		BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath, getRelativeFile("main.txt")));
		manager.require(req);
		return manager;
	}
	
	protected abstract BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input);
	
	
}
