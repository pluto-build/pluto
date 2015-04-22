package build.pluto.test.build;

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
import org.sugarj.common.path.RelativePath;

import build.pluto.BuildUnit;
import build.pluto.output.None;
import build.pluto.test.build.once.SimpleBuilder;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.once.SimpleRequirement;

public class RebuildInconsistentTest extends SimpleBuildTest{

	private File mainFile;
	private File dep1File;
	private File dep2File;
	private File dep2_1File;

	private List<File> allFiles;

	@Before
	public void makeConsistentState() throws IOException{
		mainFile = getRelativeFile("main.txt");
		dep1File = getRelativeFile("dep1.txt");
		dep2File = getRelativeFile("dep2.txt");
		dep2_1File = getRelativeFile("dep2-1.txt");
		allFiles = Arrays.asList(mainFile, dep1File, dep2File, dep2_1File);
		buildClean();
	}
	

	@Override
	protected SimpleRequirement requirementForInput(TestBuilderInput input) {
		return new SimpleRequirement(SimpleBuilder.factory, input);
	}

	
	private void buildClean() throws IOException {
		TrackingBuildManager manager = buildMainFile();
		// Now require that all compilationUnits are consistent
		for (File file : allFiles) {
			BuildUnit<None> unit = unitForFile(file, testBasePath);
			assertNotNull(
					"No unit was persisted for path: " + file,
					unit);
			assertTrue("Unit for " + file
					+ " is not consistent",
					unit.isConsistent(null));
		}
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2File, dep1File));
		validateThat(in(requiredFilesOf(manager)).is(dep2File).before(dep2_1File));

		validateThat(in(executedFilesOf(manager)).is(mainFile).before(dep1File, dep2File));
		validateThat(in(executedFilesOf(manager)).is(dep2File).before(dep2_1File));
	}

	@Test
	public void testBuildRootInconsistent() throws IOException {

		addInputFileContent(mainFile, "New content");
		assertFalse("Main file is not inconsistent after change",
				unitForFile(mainFile, testBasePath).isConsistent(null));
		// Rebuild
		TrackingBuildManager manager = new TrackingBuildManager();
		buildMainFile(manager);

		validateThat(requiredFilesOf(manager).containsAll(mainFile, dep2File, dep2_1File));
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2File, dep1File));
		validateThat(executedFilesOf(manager).containsSameElements(mainFile));
	}

	@Test
	public void testBuildLeafInconsistent() throws IOException {

		addInputFileContent(dep2_1File, "New content");
		assertFalse("dep2_1File file is not inconsistent after change",
				unitForFile(dep2_1File, testBasePath).isConsistent(null));
		// Rebuild
		TrackingBuildManager manager = buildMainFile();
	
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2_1File));
		validateThat(executedFilesOf(manager).containsSameElements(dep2_1File));

	}

	@Test
	public void testBuildLeafInconsistentNewDep() throws IOException {


		addInputFileContent(dep1File, "New content");
		
		addInputFileDep(dep2_1File, dep1File);
		assertFalse("dep2_1File file is not inconsistent after change",
				unitForFile(dep2_1File, testBasePath).isConsistent(null));
		assertFalse("dep1File file is not inconsistent after change",
				unitForFile(dep1File, testBasePath).isConsistent(null));
		
		// Rebuild
		TrackingBuildManager manager = buildMainFile();
		
		validateThat(in(requiredFilesOf(manager)).is(mainFile).before(dep2_1File));
		validateThat(executedFilesOf(manager).containsSameElements(dep2_1File, dep1File));
		
		// Cannot validate the order, because dep1File may be scheduled before 2_1
		// but 2_1 can require dep1 if 2_1 is scheduled before 1;
	}

}
