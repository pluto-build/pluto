package build.pluto.test.build.cycle.fixpoint.test;

import java.io.File;
import java.io.IOException;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.test.build.cycle.fixpoint.IntegerOutput;

@RunWith(Suite.class)
@SuiteClasses({NoCycleTest.class,GCDHomogeneousCycleTest.class, GCDMultipleCyclesTest.class})
public class FixpointCycleTestSuite {

	static final String FIXPOINT_BUILDER_CYCLE_TEST = "FixpointBuilderCycleTest";

	static BuildUnit<IntegerOutput> unitForFile(File path) throws IOException {
		return BuildUnit.read(FileCommands.addExtension(path.toPath(), "dep").toFile());
	}
	
}
