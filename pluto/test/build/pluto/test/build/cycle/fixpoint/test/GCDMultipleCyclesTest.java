package build.pluto.test.build.cycle.fixpoint.test;

import static build.pluto.test.build.UnitValidators.unitsForPath;
import static build.pluto.test.build.Validators.requiredFilesOf;
import static build.pluto.test.build.Validators.successfulyExecutedFilesOf;
import static build.pluto.test.build.Validators.validateThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.path.RelativePath;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.TrackingBuildManager;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.cycle.fixpoint.FileUtils;
import build.pluto.test.build.cycle.fixpoint.ModuloBuilder;

public class GCDMultipleCyclesTest extends ScopedBuildTest{

	private RelativePath main;
	private RelativePath cycle1;
	private RelativePath cycle2;
	private RelativePath cycle3;
	private RelativePath leaf;
	
	private RelativePath mainDep;
	private RelativePath cycle1Dep;
	private RelativePath cycle2Dep;
	private RelativePath cycle3Dep;
	private RelativePath leafDep;
	
	@Before
	public void initFiles() {
		main = getRelativeFile("main.modulo");
		cycle1 = getRelativeFile("cycle1.gcd");
		cycle2 = getRelativeFile("cycle2.gcd");
		cycle3 = getRelativeFile("cycle3.gcd");
		leaf = getRelativeFile("leaf.gcd");
		mainDep = getRelativeFile("main.modulo.dep");
		cycle1Dep = getRelativeFile("cycle1.gcd.dep");
		cycle2Dep = getRelativeFile("cycle2.gcd.dep");
		cycle3Dep = getRelativeFile("cycle3.gcd.dep");
		leafDep = getRelativeFile("leaf.gcd.dep");
	}
	
	@Override
	protected String getTestFolderName() {
		return this.getClass().getSimpleName();
	}

	private TrackingBuildManager build() throws IOException {
		TrackingBuildManager manager = new TrackingBuildManager();
		manager.require(ModuloBuilder.factory, new FileInput(testBasePath, main));
		
		validateThat(unitsForPath(mainDep, cycle1Dep, cycle2Dep, cycle3Dep, leafDep).areConsistent());
		validateThat(unitsForPath(mainDep).dependsOn(cycle1Dep));
		validateThat(unitsForPath(cycle1Dep).dependsOn(cycle2Dep));
		validateThat(unitsForPath(cycle2Dep).dependsOn(cycle1Dep, cycle3Dep, leafDep));
		validateThat(unitsForPath(cycle3Dep).dependsOn(cycle1Dep));
		
		return manager;
	}
	

	@Test (timeout = 1000)
	public void testBuildClean() throws IOException{
		TrackingBuildManager manager = build();

		// Only leaf, main and cycle1 should be executed successfully by normal manager
		// cycle1 is successful because it initiated the cycle build, so returns normally
		validateThat(successfulyExecutedFilesOf(manager).containsSameElements(main, leaf, cycle1));
	}
	
	@Test (timeout = 1000)
	public void testRebuildInconsistentLeafSource() throws IOException{
		build();
		
		System.out.println();
		System.out.println();
		
		// Make the source of the leaf inconsistent
		FileUtils.writeIntToFile(81, leaf);
		
		TrackingBuildManager manager = build();
		validateThat(successfulyExecutedFilesOf(manager).containsSameElements(leaf, main, cycle2));
		validateThat(requiredFilesOf(manager).containsSameElements(main, cycle1, cycle2, cycle3, leaf));
	}
	
	@Test (timeout = 1000)
	public void testRebuildInconsistentCycle3Source() throws IOException{
		build();
		
		System.out.println();
		System.out.println();
		
		// Make the source of the leaf inconsistent
		FileUtils.writeIntToFile(81, cycle3);
		
		TrackingBuildManager manager = build();
		validateThat(successfulyExecutedFilesOf(manager).containsSameElements(main, cycle3, cycle1));
		validateThat(requiredFilesOf(manager).containsSameElements(main, cycle1, cycle2, cycle3, leaf));
	}
	
	
	
}
