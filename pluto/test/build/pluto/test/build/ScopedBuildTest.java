package build.pluto.test.build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.sugarj.common.FileCommands;

public abstract class ScopedBuildTest {

  @Rule
  public TestName name = new TestName();

  private Path basePath;
  protected Path testBasePath;

  protected File getRelativeFile(String name) {
    return testBasePath.resolve(name).toFile();
  }

  protected String getTestFolderName() {
    return this.getClass().getSimpleName();
  }

  @Before
  public void initializeTestEnvironment() throws IOException {
    basePath = Paths.get("testdata", getTestFolderName()).toAbsolutePath();
    testBasePath = basePath.resolve(name.getMethodName());

    FileCommands.delete(testBasePath);
    FileCommands.createDir(testBasePath);

    Files.list(basePath).filter(((Predicate<Path>) Files::isDirectory).negate()).forEach((Path path) -> {
      try {
        Files.copy(path, testBasePath.resolve(path.getFileName()));
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });

    injectScopedPaths();

    System.out.println();
    System.out.println("====== Execute test " + name.getMethodName() + " ======");
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
        File relPath = getRelativeFile(pathValue);
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

  protected String fieldNameToPathName(String fieldName) {
    String result = "";
    for (char c : fieldName.toCharArray()) {
      if (Character.isUpperCase(c)) {
        result += "_" + Character.toLowerCase(c);
      } else {
        result += c;
      }
    }
    result += ".txt";
    return result;
  }

}
