package build.pluto.test.build.cycle.fixpoint.test;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

import build.pluto.BuildUnit;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.cycle.fixpoint.IntegerOutput;
import build.pluto.test.build.cycle.fixpoint.ModuloBuilder;

public class NoCycleTest extends ScopedBuildTest{

	@Override
	protected String getTestFolderName() {
		return FixpointCycleTestSuite.FIXPOINT_BUILDER_CYCLE_TEST;
	}
	
	@Test
	public void testBuildNoCycle() throws IOException {
		BuildUnit<IntegerOutput> resultUnit = new TrackingBuildManager().require(ModuloBuilder.factory, new FileInput(testBasePath.toFile(), "main1.modulo")).getUnit();
		assertEquals("No cycle produced wrong output", 1, resultUnit.getBuildResult().getResult());
	}

}
