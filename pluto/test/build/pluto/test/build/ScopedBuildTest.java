package build.pluto.test.build;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;

public abstract class ScopedBuildTest {

	

	@Rule
	public TestName name = new TestName();
	

	private AbsolutePath basePath ;
	protected AbsolutePath testBasePath;
	
	protected RelativePath getRelativeFile(String name) {
		return new RelativePath(testBasePath, name);
	}
	
	protected abstract String getTestFolderName();

	@Before
	public void initializeTestEnvironment() throws IOException {
		basePath = new AbsolutePath(new File(
				"testdata/"+getTestFolderName()).getAbsolutePath());
		testBasePath = new AbsolutePath(basePath.getAbsolutePath() + "/"
				+ name.getMethodName());
		
		FileCommands.delete(testBasePath);
		FileCommands.createDir(testBasePath);
		

		for (RelativePath path : FileCommands.listFiles(basePath,
				new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						return pathname.isFile();
					}
				})) {
			FileCommands.copyFile(path,
					new RelativePath(testBasePath, path.getRelativePath()));
		}
		
		System.out.println();
		System.out.println("====== Execute test " + name.getMethodName() + " ======");
	}
	
}
