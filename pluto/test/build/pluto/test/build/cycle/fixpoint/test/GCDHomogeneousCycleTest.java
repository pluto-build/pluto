package build.pluto.test.build.cycle.fixpoint.test;

import static build.pluto.test.build.Validators.executedFilesOf;
import static build.pluto.test.build.Validators.in;
import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.cycle.fixpoint.test.FixpointCycleTestSuite.unitForFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.RelativePath;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManager;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.cycle.fixpoint.FileUtils;
import build.pluto.test.build.cycle.fixpoint.IntegerOutput;
import build.pluto.test.build.cycle.fixpoint.ModuloBuilder;


public class GCDHomogeneousCycleTest extends ScopedBuildTest {

	private RelativePath mainFile;
	private RelativePath cycle_gcd1File;
	private RelativePath cycle_gcd2File;
	private BuildRequest<FileInput, IntegerOutput, ModuloBuilder, BuilderFactory<FileInput, IntegerOutput, ModuloBuilder>> mainBuildRequest;
	
	@Before
	public void initFiles() {
		mainFile = getRelativeFile("cyclemodmain.modulo");
		cycle_gcd1File = getRelativeFile("cycle_gcd1.gcd");
		cycle_gcd2File = getRelativeFile("cycle_gcd2.gcd");
		mainBuildRequest  = new BuildRequest<>(
				ModuloBuilder.factory, new FileInput(testBasePath, mainFile));
	}
	
	@Override
	protected String getTestFolderName() {
		return FixpointCycleTestSuite.FIXPOINT_BUILDER_CYCLE_TEST;
	}
	
	private void assertAllFilesConsistent() throws IOException{
		for (RelativePath path : Arrays.asList(mainFile, cycle_gcd1File, cycle_gcd2File)) {
			assertTrue("File " + path.getRelativePath() + " is not consistent", unitForFile(path).isConsistent(null));
		}
 	}

	@Test (timeout = 1000)
	public void testBuildGCDCycle() throws IOException {
		BuildUnit<IntegerOutput> resultUnit = new TrackingBuildManager()
				.require(mainBuildRequest);
		assertEquals("Compiliding GCD cycle has wrong result", 0, resultUnit
				.getBuildResult().getResult());
		assertAllFilesConsistent();
	}

	@Test (timeout = 1000)
	public void testRebuildRootUnitInconsistent() throws IOException {

		// Do a first clean build
		BuildManager.build(mainBuildRequest);

		// Then make the root inconsistent
		FileUtils.writeIntToFile(19, mainFile);

		TrackingBuildManager manager = new TrackingBuildManager();
		BuildUnit<IntegerOutput> resultUnit = manager.require(mainBuildRequest);
		// Assert that the new result is correct
		assertEquals("Rebuilding GCD cycle with inconsistent has wrong result", 4, resultUnit
				.getBuildResult().getResult());
		
		// Primitive check
		assertAllFilesConsistent();
		
		// Check that only main is executed
		validateThat(executedFilesOf(manager).containsSameElements(mainFile));
		
		// And that main is required before the other ones. Because main refers to gcd1, this should be before gcd2
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(cycle_gcd1File));
		validateThat(in(requiredFilesOf(manager)).is(cycle_gcd1File).before(cycle_gcd2File));

	}
	
	@Test (timeout = 1000)
	public void testRebuildCycle1UnitInconsistent() throws IOException {

		// Do a first clean build
		BuildManager.build(mainBuildRequest);
		assertAllFilesConsistent();

		// Then make the cycle1 inconsistent
		FileCommands.delete(unitForFile(cycle_gcd1File).getBuildResult().getResultFile());

		TrackingBuildManager manager = new TrackingBuildManager();
		BuildUnit<IntegerOutput> resultUnit = manager.require(mainBuildRequest);
		// Assert that the new result is correct
		assertEquals("Rebuilding GCD cycle with inconsistent has wrong result", 0, resultUnit
				.getBuildResult().getResult());
		
		// Primitive check
		assertAllFilesConsistent();
		
		// Check that only the cycle is executed
		// Main must not be executed because the cycle produces the same result as before
		validateThat(executedFilesOf(manager).containsSameElements(cycle_gcd1File, cycle_gcd2File));
		
		// And that main is required before the other ones. Because main refers to gcd1, this should be before gcd2
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(cycle_gcd1File));
		validateThat(in(requiredFilesOf(manager)).is(cycle_gcd1File).before(cycle_gcd2File));

	}

}
