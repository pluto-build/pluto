package build.pluto.test.build;

import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.RelativePath;

import build.pluto.builder.BuildRequest;
import build.pluto.test.build.once.SimpleBuilder;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleRequirement;

public class BuildFailureTest extends SimpleBuildTest {

  @Override
  protected BuildRequest<?, ?, ?, ?> requirementForInput(TestBuilderInput input) {
    return new SimpleRequirement(SimpleBuilder.factory, input);
  }
  
  private RelativePath mainFile;
  private RelativePath dep1File;
  private RelativePath dep1FileGood;
  private RelativePath dep1FileFail;
  private RelativePath dep2File;
  private List<RelativePath> allFiles;
  
  @Before
  public void makeConsistentState() throws IOException{
    mainFile = getRelativeFile("main.txt");
    dep1File = getRelativeFile("dep1.txt");
    dep1FileGood = getRelativeFile("dep1-good.txt");
    dep1FileFail = getRelativeFile("dep1-fail.txt");
    dep2File = getRelativeFile("dep2.txt");
    allFiles = Arrays.asList(mainFile, dep1File, dep2File);
  }
  
  @Test
  public void testSuccessfulBuild() throws IOException {
    FileCommands.copyFile(dep1FileGood, dep1File);
    TrackingBuildManager manager = new TrackingBuildManager();
    buildMainFile(manager);

    validateThat(requiredFilesOf(manager).containsAll(mainFile, dep1File, dep2File));
  }
  
  @Test
  public void testFailedBuild() throws IOException {
    FileCommands.copyFile(dep1FileFail, dep1File);
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    
    // no dep2File
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));
  }
  
  @Test
  public void testSuccessAfterFailureBuild() throws IOException {
    FileCommands.copyFile(dep1FileFail, dep1File);
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));
    
    FileCommands.copyFile(dep1FileGood, dep1File);
    manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep2File));
  }

  @Test
  public void testFailureAfterSuccessBuild() throws IOException {
    FileCommands.copyFile(dep1FileGood, dep1File);
    TrackingBuildManager manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File, dep2File));
    
    FileCommands.copyFile(dep1FileFail, dep1File);
    manager = new TrackingBuildManager();
    try {
      buildMainFile(manager);
    } catch (Exception e) {
    }
    
    validateThat(requiredFilesOf(manager).containsSameElements(mainFile, dep1File));
  }
}
