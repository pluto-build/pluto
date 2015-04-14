package build.pluto.test.build;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;

public abstract class ScopedBuildTest {

	@Rule
	public TestName name = new TestName();

	private AbsolutePath basePath;
	protected AbsolutePath testBasePath;

	protected RelativePath getRelativeFile(String name) {
		return new RelativePath(testBasePath, name);
	}

	protected abstract String getTestFolderName();

	@Before
	public void initializeTestEnvironment() throws IOException {
		basePath = new AbsolutePath(
				new File("testdata/" + getTestFolderName()).getAbsolutePath());
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

		injectScopedPaths();

		System.out.println();
		System.out.println("====== Execute test " + name.getMethodName()
				+ " ======");
	}

	private void injectScopedPaths() {
		Class<? extends ScopedBuildTest> clazz = this.getClass();
		System.out.println("Clazz " + clazz);
		for (Field field : clazz.getDeclaredFields()) {
			String pathValue = null;
			ScopedPath path = field.getAnnotation(ScopedPath.class);
			if (path != null) {
				pathValue = path.value();
			}
			DefaultNamedScopedPath defPath = field.getAnnotation(DefaultNamedScopedPath.class);
			if (defPath != null) {
				pathValue = fieldNameToPathName(field.getName());
			}
			if (pathValue != null) {
				
				boolean access = field.isAccessible();
				field.setAccessible(true);
				RelativePath relPath = getRelativeFile(pathValue);
				try {
					field.set(this, relPath);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				field.setAccessible(access);
			}
		}
	}
	
	protected String fieldNameToPathName(String fieldName)  {
		String result = "";
		for (char c : fieldName.toCharArray()) {
			if (Character.isUpperCase(c)) {
				result += "_" + Character.toLowerCase(c);
			} else {
				result +=c;
			}
		}
		result += ".txt";
		return result;
	}

}
