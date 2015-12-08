package build.pluto.test.build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

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
  
  protected Collection<String> alsoCopyDirs() {
    return Collections.emptyList();
  }

  @Before
  public void initializeTestEnvironment() throws IOException {
    Log.log.setLoggingLevel(Log.ALWAYS);
    basePath = Paths.get("testdata", getTestFolderName()).toAbsolutePath();
    testBasePath = basePath.resolve(name.getMethodName());

    FileCommands.delete(testBasePath);
    FileCommands.createDir(testBasePath);

    Collection<String> copyDirs = alsoCopyDirs();
    
    for (File file : basePath.toFile().listFiles())
      if (!file.isDirectory())
        try {
          Files.copy(file.toPath(), testBasePath.resolve(file.getName()));
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      else if (copyDirs.contains(file.getName())) {
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
          private Path currentDir = testBasePath;
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Files.copy(dir, testBasePath.resolve(dir.toFile().getName()));
            currentDir = currentDir.resolve(dir.toFile().getName());
            return FileVisitResult.CONTINUE;
          }
          
          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            currentDir = currentDir.getParent();
            return FileVisitResult.CONTINUE;
          }
          
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            System.out.println("COPY " + file + " to " + currentDir);
            Files.copy(file, currentDir.resolve(file.toFile().getName()));
            return FileVisitResult.CONTINUE;
          }
        });
      }

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
