package build.pluto.test.build.cycle.fixpoint.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import build.pluto.BuildUnit;
import build.pluto.output.OutputPersisted;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.cycle.fixpoint.ModuloBuilder;

public class NoCycleTest extends ScopedBuildTest{

	@Override
	protected String getTestFolderName() {
		return FixpointCycleTestSuite.FIXPOINT_BUILDER_CYCLE_TEST;
	}
	
	@Test
	public void testBuildNoCycle() throws IOException {
    BuildUnit<OutputPersisted<Integer>> resultUnit = new TrackingBuildManager().require(ModuloBuilder.factory, new FileInput(testBasePath.toFile(), "main1.modulo")).getUnit();
    assertEquals("No cycle produced wrong output", 1, resultUnit.getBuildResult().val.intValue());
	}

}
