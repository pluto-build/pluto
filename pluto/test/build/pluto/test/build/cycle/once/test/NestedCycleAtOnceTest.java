package build.pluto.test.build.cycle.once.test;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.sugarj.common.path.RelativePath;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CompileCycleAtOnceBuilder;
import build.pluto.output.None;
import build.pluto.test.build.DefaultNamedScopedPath;
import build.pluto.test.build.SimpleBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleCyclicAtOnceBuilder;

import static build.pluto.test.build.once.SimpleBuildUtilities.*;
import static build.pluto.test.build.Validators.*;

public class NestedCycleAtOnceTest extends SimpleBuildTest{

	@DefaultNamedScopedPath
	private RelativePath main;
	
	@DefaultNamedScopedPath
	private RelativePath cyclePart;
	
	@DefaultNamedScopedPath
	private RelativePath cycleEntry;
	
	@DefaultNamedScopedPath
	private RelativePath subcycleEntry;
	
	@DefaultNamedScopedPath
	private RelativePath subcyclePart1;
	
	@DefaultNamedScopedPath
	private RelativePath subcyclePart2;
	
	
	@Override
	protected BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input) {
		return new BuildRequest<ArrayList<TestBuilderInput>,None, SimpleCyclicAtOnceBuilder, BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder>> (SimpleCyclicAtOnceBuilder.factory, CompileCycleAtOnceBuilder.singletonArrayList(input));
	}
	
	@Test(timeout=2000)
	public void testCleanRebuild() throws IOException {
		TrackingBuildManager manager = buildMainFile();
		
		validateThat(executedFilesOf(manager).containsSameElements(main, cycleEntry, cyclePart, subcycleEntry, subcyclePart1));
	}
	
	@Test(timeout=2000)
	public void testRebuildWithChangedCycleStructure() throws IOException {
		testCleanRebuild();
		
		removeInputFileDep(subcycleEntry, subcyclePart1);
		addInputFileDep(subcycleEntry, subcyclePart2);
		
		TrackingBuildManager manager = buildMainFile();
		
		validateThat(executedFilesOf(manager).containsSameElements(cycleEntry, cyclePart, subcycleEntry, subcyclePart2));
	
		
	}

}
