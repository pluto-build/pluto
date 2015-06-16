package build.pluto.test.build.cycle.fixpoint.test;

import static build.pluto.test.build.Validators.executedFilesOf;
import static build.pluto.test.build.Validators.in;
import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.cycle.fixpoint.test.FixpointCycleTestSuite.unitForFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.OutputPersisted;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.cycle.fixpoint.FileUtils;
import build.pluto.test.build.cycle.fixpoint.ModuloBuilder;

public class GCDHomogeneousCycleTest extends ScopedBuildTest {

  @ScopedPath("cyclemodmain.modulo")
  private File mainFile;
  @ScopedPath("cycle_gcd1.gcd")
  private File cycle_gcd1File;
  @ScopedPath("cycle_gcd2.gcd")
  private File cycle_gcd2File;
  private BuildRequest<FileInput, OutputPersisted<Integer>, ModuloBuilder, BuilderFactory<FileInput, OutputPersisted<Integer>, ModuloBuilder>> mainBuildRequest;

  @Before
  public void initFiles() {
    mainBuildRequest = new BuildRequest<>(ModuloBuilder.factory, new FileInput(testBasePath.toFile(), mainFile));
  }

  @Override
  protected String getTestFolderName() {
    return FixpointCycleTestSuite.FIXPOINT_BUILDER_CYCLE_TEST;
  }

  private void assertAllFilesConsistent() throws IOException {
    for (File path : Arrays.asList(mainFile, cycle_gcd1File, cycle_gcd2File)) {
      assertTrue("File " + path + " is not consistent", unitForFile(path).isConsistent());
    }
  }

  @Test(timeout = 1000)
  public void testBuildGCDCycle() throws IOException {
    assertEquals(10, BigInteger.valueOf(20).gcd(BigInteger.valueOf(10)).intValue());
    new TrackingBuildManager().require(mainBuildRequest).getUnit();
    assertEquals("Compiling GCD cycle has wrong result", 5, unitForFile(cycle_gcd2File).getBuildResult().val.intValue());
    assertEquals("Compiling GCD cycle has wrong result", 5, unitForFile(cycle_gcd1File).getBuildResult().val.intValue());
    assertEquals("Compiling GCD cycle has wrong result", 0, unitForFile(mainFile).getBuildResult().val.intValue());
    assertAllFilesConsistent();
  }

  @Test(timeout = 1000)
  public void testRebuildRootUnitInconsistent() throws IOException {

    Log.log.setLoggingLevel(Log.DETAIL | Log.CORE);
    BuildManagers.build(mainBuildRequest);

    // Then make the root inconsistent
    FileUtils.writeIntToFile(19, mainFile);

    TrackingBuildManager manager = new TrackingBuildManager();
    BuildUnit<OutputPersisted<Integer>> resultUnit = manager.require(mainBuildRequest).getUnit();
    // Assert that the new result is correct
    assertEquals("Rebuilding GCD cycle with inconsistent root unit has wrong result", 4, resultUnit.getBuildResult().val.intValue());

    // Primitive check
    assertAllFilesConsistent();

    // Check that only main is executed
    validateThat(executedFilesOf(manager).containsSameElements(mainFile));

    // And that main is required before the other ones. Because main refers to
    // gcd1, this should be before gcd2
    validateThat(in(requiredFilesOf(manager)).is(mainFile).before(cycle_gcd1File));
    validateThat(in(requiredFilesOf(manager)).is(cycle_gcd1File).before(cycle_gcd2File));

  }

  @Test(timeout = 1000)
  public void testRebuildCycle1UnitInconsistent() throws IOException {

    // Do a first clean build
    BuildManagers.build(mainBuildRequest);
    assertAllFilesConsistent();

    // Then make the cycle1 inconsistent
    FileCommands.delete(unitForFile(cycle_gcd1File).getPersistentPath().toPath());

    Log.log.setLoggingLevel(Log.ALWAYS);

    TrackingBuildManager manager = new TrackingBuildManager();
    BuildUnit<OutputPersisted<Integer>> resultUnit = manager.require(mainBuildRequest).getUnit();
    // Assert that the new result is correct
    assertEquals("Rebuilding GCD cycle with inconsistent has wrong result", 0, resultUnit.getBuildResult().val.intValue());

    // Primitive check
    assertAllFilesConsistent();

    // Check that only the cycle is executed
    // Main must not be executed because the cycle produces the same result as
    // before
    validateThat(executedFilesOf(manager).containsSameElements(cycle_gcd1File));

    // And that main is required before the other ones. Because main refers to
    // gcd1, this should be before gcd2
    validateThat(in(requiredFilesOf(manager)).is(mainFile).before(cycle_gcd1File));
    validateThat(in(requiredFilesOf(manager)).is(cycle_gcd1File).before(cycle_gcd2File));

  }

}
