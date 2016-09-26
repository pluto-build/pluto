package build.pluto.test.build;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.test.build.once.SimpleBuilder;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleRequirement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static org.junit.Assert.assertTrue;

public class BuildFailureTestWithDepDiscovery extends SimpleBuildTest {

  @Override
  protected BuildRequest<?, ?, ?, ?> requirementForInput(TestBuilderInput input) {
    return new SimpleRequirement(SimpleBuilder.factoryFileDepDiscovery, input);
  }
  
  private File mainFile;
  private File dep1File;
  private File dep1FileGood;
  private File dep1FileFail;
  private File dep2File;
  
  @Before
  public void makeConsistentState() throws IOException{
    mainFile = getRelativeFile("main.txt");
    dep1File = getRelativeFile("dep1.txt");
    dep1FileGood = getRelativeFile("dep1-good.txt");
    dep1FileFail = getRelativeFile("dep1-fail.txt");
    dep2File = getRelativeFile("dep2.txt");
  }
  
  @Test
  public void testSuccessfulBuild() throws IOException {
    Files.copy(dep1FileGood.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    buildMainFile(manager);

    validateThat(requiredFilesOf(manager).containsAll(mainFile, dep1File, dep2File));
  }
  
  @Test
  public void testFailedBuild() throws IOException {
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }

    // no dep2File
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));

    BuildRequest<?,?,?,?> req = requirementForInput(new TestBuilderInput(testBasePath.toFile(), mainFile));
    BuildUnit<?> unit = BuildManagers.readResult(req);
    assertTrue(unit.isConsistent());
  }

  @Test
  public void testSuccessAfterFailureBuild() throws IOException {
    Log.log.setLoggingLevel(Log.ALWAYS);
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));
    manager.getTracer().stop();

    Files.copy(dep1FileGood.toPath(), dep1File.toPath(), StandardCopyOption.REPLACE_EXISTING);
    manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }

    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep2File));
  }

  @Test
  public void testFailureAfterSuccessBuild() throws IOException {
    Files.copy(dep1FileGood.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep2File));
    manager.getTracer().stop();

    Files.copy(dep1FileFail.toPath(), dep1File.toPath(), StandardCopyOption.REPLACE_EXISTING);
    manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }

    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));
  }

  @Test
  public void testFailureAfterFailureBuild() throws IOException {
    Files.copy(dep1FileFail.toPath(), dep1File.toPath());
    TrackingBuildManager manager = new TrackingBuildManager();
    boolean failed = false;
    try {
      buildMainFile(manager);
    } catch (Exception e) {
      failed = true;
    }
    Assert.assertTrue(failed);

    manager = new TrackingBuildManager();
    failed = false;
    try {
      buildMainFile(manager);
    } catch (Exception e) {
      failed = true;
    }
    Assert.assertTrue(failed);
  }
}
