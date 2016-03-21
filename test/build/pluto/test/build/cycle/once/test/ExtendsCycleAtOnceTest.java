package build.pluto.test.build.cycle.once.test;

import static build.pluto.test.build.Validators.executedFilesOf;
import static build.pluto.test.build.Validators.validateThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.None;
import build.pluto.test.build.DefaultNamedScopedPath;
import build.pluto.test.build.SimpleBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleCyclicAtOnceBuilder;

public class ExtendsCycleAtOnceTest extends SimpleBuildTest{

	@DefaultNamedScopedPath
	private File main;
	
  @DefaultNamedScopedPath
  private File cycle1;

  @DefaultNamedScopedPath
  private File cycle2;

  @DefaultNamedScopedPath
  private File cycle_ex;

  @DefaultNamedScopedPath
  private File cycle4;

	
	
	@Override
	protected BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input) {
		return new BuildRequest<ArrayList<TestBuilderInput>,None, SimpleCyclicAtOnceBuilder, BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder>> (SimpleCyclicAtOnceBuilder.factory, BuildCycleAtOnceBuilder.singletonArrayList(input));
	}
	
	@Test(timeout=2000)
	public void testCleanRebuild() throws IOException {
		TrackingBuildManager manager = buildMainFile();
		
    validateThat(executedFilesOf(manager).containsSameElements(main, cycle1, cycle2, cycle_ex, cycle4));
	}

}
