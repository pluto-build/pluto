package build.pluto.test.build.output;

import static build.pluto.test.build.Validators.executedFilesOf;
import static build.pluto.test.build.Validators.in;
import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.once.SimpleBuildUtilities.addInputFileContent;
import static build.pluto.test.build.once.SimpleBuildUtilities.addInputFileDep;
import static build.pluto.test.build.once.SimpleBuildUtilities.unitForFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.None;
import build.pluto.output.Out;
import build.pluto.output.Output;
import build.pluto.output.OutputTransient;
import build.pluto.test.build.SimpleBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class OutputTransientTest extends SimpleBuildTest {

  
  public static class OutputTransientBuilder extends SimpleOutputBuilder {
    public static BuilderFactory<TestBuilderInput, Output, SimpleOutputBuilder> factory = BuilderFactory.of(OutputTransientBuilder.class, TestBuilderInput.class);

    public OutputTransientBuilder(TestBuilderInput input) {
      super(input);
    }

    @Override
    protected Output output(TestBuilderInput input) {
      return OutputTransient.of(input.getInputPath());
    }

    @Override
    protected BuilderFactory<TestBuilderInput, Output, SimpleOutputBuilder> factory() {
      return factory;
    }

    @Override
    protected void checkOutput(SimpleOutputBuilderRequirement req, Output reqOut) {
      if (reqOut instanceof Out<?> && ((Out<?>) reqOut).val() == null)
        throw new AssertionError("Output of requirement " + req + " was null.");
      else if (!(reqOut instanceof Out<?>))
        throw new AssertionError("Output of requirement " + req + " does not contain value.");
    }

    
  }
  
  private File mainFile;
  private File dep1File;
  private File dep2File;
  private File dep2_1File;

  private List<File> allFiles;

  @Before
  public void makeConsistentState() throws IOException {
    mainFile = getRelativeFile("main.txt");
    dep1File = getRelativeFile("dep1.txt");
    dep2File = getRelativeFile("dep2.txt");
    dep2_1File = getRelativeFile("dep2-1.txt");
    allFiles = Arrays.asList(mainFile, dep1File, dep2File, dep2_1File);
    buildClean();
  }

  @Override
  protected BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input) {
    return new SimpleOutputBuilderRequirement(OutputTransientBuilder.factory, input);
  }
  
  private void checkAllOutputs() throws IOException {
    for (File file : allFiles) {
      BuildUnit<None> unit = unitForFile(file, testBasePath);
      assertNotNull("No unit was persisted for path: " + file, unit);
      assertTrue("Unit for " + file + " is not consistent", unit.isConsistent());
      assertNotNull("Output is null", unit.getBuildResult());
    }
  }
  


  private void buildClean() throws IOException {
    buildMainFile();
    checkAllOutputs();
  }

  @Test
  public void testBuildRootConsistent() throws IOException {
    TrackingBuildManager manager = new TrackingBuildManager();
    buildMainFile(manager);
    validateThat(executedFilesOf(manager).hasSize(0));
    checkAllOutputs();
  }

  @Test
  public void testBuildRootInconsistent() throws IOException {

    addInputFileContent(mainFile, "New content");
    assertFalse("Main file is not inconsistent after change", unitForFile(mainFile, testBasePath).isConsistent());
    // Rebuild
    TrackingBuildManager manager = new TrackingBuildManager();
    buildMainFile(manager);

    validateThat(requiredFilesOf(manager).containsAll(mainFile, dep2File, dep2_1File));
    validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2File, dep1File));
    validateThat(executedFilesOf(manager).containsSameElements(mainFile));
    checkAllOutputs();
  }

  @Test
  public void testBuildLeafInconsistent() throws IOException {

    addInputFileContent(dep2_1File, "New content");
    assertFalse("dep2_1File file is not inconsistent after change", unitForFile(dep2_1File, testBasePath).isConsistent());
    // Rebuild
    TrackingBuildManager manager = buildMainFile();

    validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2_1File));
    validateThat(executedFilesOf(manager).containsSameElements(dep2_1File));
    checkAllOutputs();
  }

  @Test
  public void testBuildLeafInconsistentNewDep() throws IOException {

    addInputFileContent(dep1File, "New content");

    addInputFileDep(dep2_1File, dep1File);
    assertFalse("dep2_1File file is not inconsistent after change", unitForFile(dep2_1File, testBasePath).isConsistent());
    assertFalse("dep1File file is not inconsistent after change", unitForFile(dep1File, testBasePath).isConsistent());

    // Rebuild
    TrackingBuildManager manager = buildMainFile();

    validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2_1File));
    validateThat(executedFilesOf(manager).containsSameElements(dep2_1File, dep1File));
    checkAllOutputs();
  }

}
