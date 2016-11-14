package build.pluto.test.build;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.test.build.once.SimpleBuilder;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleRequirement;
import build.pluto.tracing.*;
import org.junit.*;
import org.sugarj.common.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildInterruptTestWithDepDiscovery extends SimpleBuildTest {

  private static ITracer tracer = TracingProvider.getTracer();

  @Override
  protected BuildRequest<?, ?, ?, ?> requirementForInput(TestBuilderInput input) {
    return new SimpleRequirement(SimpleBuilder.factoryFileDepDiscovery, input);
  }
  
  private File mainFile;
  private File dep0File;
  private File dep0FileGood;
  private File dep0FileFail;
  private File dep1File;
  private File dep1FileGood;
  private File dep1FileFail;
  private File dep2File;
  
  @Before
  public void makeConsistentState() throws IOException {
    mainFile = getRelativeFile("main.txt");
    dep0File = getRelativeFile("dep0.txt");
    dep0FileGood = getRelativeFile("dep0-good.txt");
    dep0FileFail = getRelativeFile("dep0-fail.txt");
    dep1File = getRelativeFile("dep1.txt");
    dep1FileGood = getRelativeFile("dep1-good.txt");
    dep1FileFail = getRelativeFile("dep1-fail.txt");
    dep2File = getRelativeFile("dep2.txt");
    Log.log.setLoggingLevel(Log.ALWAYS);
  }

  @AfterClass
  public static void stopTracer() {
    tracer.stop();
  }
  
  @Test
  public void testSuccessfulBuild() throws IOException {
    Files.copy(dep0FileGood.toPath(), dep0File.toPath());
    Files.copy(dep1FileGood.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    buildMainFile(manager);

    validateThat(requiredFilesOf(manager).containsAll(mainFile, dep0File, dep1File, dep2File));
  }
  
  
  @Test
  public void testFailedBuild0() throws IOException {
    Files.copy(dep0FileFail.toPath(), dep0File.toPath());
    Files.copy(dep1FileGood.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (RequiredBuilderFailed e) {
      assertTrue(e.getRootCause() instanceof InterruptedException);
    }
    
    // no dep2File
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File));
    
    BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath.toFile(), mainFile));
    BuildUnit<?> unit = BuildManagers.readResult(req);
    assertFalse(unit.isConsistent());
  }
  
  @Test
  public void testFailedBuild1() throws IOException {
    Files.copy(dep0FileGood.toPath(), dep0File.toPath());
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (RequiredBuilderFailed e) {
      assertTrue(e.getRootCause() instanceof InterruptedException);
    }
    
    // no dep2File
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File));

    BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath.toFile(), mainFile));
    BuildUnit<?> unit = BuildManagers.readResult(req);
    assertFalse(unit.isConsistent());
  }

  @Test
  public void testSuccessAfterFailureBuild() throws IOException {
    Files.copy(dep0FileGood.toPath(), dep0File.toPath());
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File));
    
    Files.copy(dep1FileGood.toPath(), dep1File.toPath(), StandardCopyOption.REPLACE_EXISTING);
    manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File, dep2File));
  }

  @Test
  public void testFailureAfterSuccessBuild() throws IOException {
    Files.copy(dep0FileGood.toPath(), dep0File.toPath());
    Files.copy(dep1FileGood.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File, dep2File));
    
    Files.copy(dep1FileFail.toPath(), dep1File.toPath(), StandardCopyOption.REPLACE_EXISTING);
    manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep0File));
  }
  
  @Test
  public void testFailureAfterFailureBuild() throws IOException {
    Files.copy(dep0FileGood.toPath(), dep0File.toPath());
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    boolean failed = false;
    try {
      buildMainFile(manager);
    } catch (Exception e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    
    manager = new TrackingBuildManager();
    manager.setTracer(tracer);
    failed = false;
    try {
      buildMainFile(manager);
    } catch (Exception e) {
      failed = true;
    }
    Assert.assertTrue(failed);
  }
}
