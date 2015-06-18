package build.pluto.test.build.cycle.once.test;

import static build.pluto.test.build.Validators.executedFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.once.SimpleBuildUtilities.addInputFileDep;
import static build.pluto.test.build.once.SimpleBuildUtilities.removeInputFileDep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.output.None;
import build.pluto.test.build.DefaultNamedScopedPath;
import build.pluto.test.build.SimpleBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleCyclicAtOnceBuilder;

public class NestedCycleAtOnceTest extends SimpleBuildTest{

	@DefaultNamedScopedPath
	private File main;
	
	@DefaultNamedScopedPath
	private File cyclePart;
	
	@DefaultNamedScopedPath
	private File cycleEntry;
	
	@DefaultNamedScopedPath
	private File subcycleEntry;
	
	@DefaultNamedScopedPath
	private File subcyclePart1;
	
	@DefaultNamedScopedPath
	private File subcyclePart2;
	
	
	@Override
	protected BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input) {
		return new BuildRequest<ArrayList<TestBuilderInput>,None, SimpleCyclicAtOnceBuilder, BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder>> (SimpleCyclicAtOnceBuilder.factory, BuildCycleAtOnceBuilder.singletonArrayList(input));
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
