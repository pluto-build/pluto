package build.pluto.test.build.once;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.output.None;
import build.pluto.test.build.cycle.fixpoint.FileInput;
import build.pluto.test.build.once.SimpleBuilder.TestBuilderInput;

public class SimpleBuildUtilities {
	
	public static void addInputFileContent(File path, String newContent)
			throws IOException {
		List<String> lines = FileCommands.readFileLines(path);
		lines.add(newContent);
		FileCommands.writeLinesFile(path, lines);
	}
	
	public static void addInputFileDep(File path, File dep)
			throws IOException {
    List<String> lines = FileCommands.readFileLines(path);
		lines.add("Dep:"+dep.getName());
    FileCommands.writeLinesFile(path, lines);
	}
	
	public static void removeInputFileDep(File path, File dep)
			throws IOException {
    List<String> lines = FileCommands.readFileLines(path);
		lines.remove("Dep:"+dep.getName());
    FileCommands.writeLinesFile(path, lines);
	}

	public static BuildUnit<None> unitForFile(File path, Path testBasePath)
			throws IOException {
		SimpleRequirement req = new SimpleRequirement(SimpleBuilder.factory,new TestBuilderInput(testBasePath.toFile(), path));
		File p = req.factory.makeBuilder(req.input).persistentPath(req.input);
		p.setLastModified(p.lastModified() + 10000);
    BuildUnit<None> unit = BuildUnit.read(p);
		return unit;
	}

	@SuppressWarnings("unchecked")
  public static List<File> inputToFileList(List<Serializable> inputs) {
		ArrayList<File> fileList = new ArrayList<>();
		for (Serializable s : inputs) {
			if (s instanceof TestBuilderInput) {
				fileList.add(((TestBuilderInput) s).getInputPath());
			} else if (s instanceof FileInput){
				fileList.add(((FileInput) s).getFile());
			} else if (s instanceof ArrayList) {
				fileList.addAll(inputToFileList((ArrayList<Serializable>) s));
      } else if (s instanceof File) {
        fileList.add((File) s);
			} else {
				fail("Illegal input " + s.getClass());
			}
		}
		return fileList;
	}

}
