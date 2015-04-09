package build.pluto.test.build;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

import build.pluto.BuildUnit;
import build.pluto.output.None;
import build.pluto.test.build.SimpleBuilder.TestBuilderInput;
import build.pluto.test.build.cycle.fixpoint.FileInput;

public class SimpleBuildUtilities {
	
	public static void addInputFileContent(RelativePath path, String newContent)
			throws IOException {
		List<String> lines = FileCommands.readFileLines(path);
		lines.add(newContent);
		FileCommands.writeLinesFile(path, lines);
	}
	
	public static void addInputFileDep(RelativePath path, RelativePath dep)
			throws IOException {
		List<String> lines = FileCommands.readFileLines(path);
		lines.add("Dep:"+dep.getRelativePath());
		FileCommands.writeLinesFile(path, lines);
	}

	public static BuildUnit<None> unitForFile(RelativePath path, Path testBasePath)
			throws IOException {
		TestRequirement req = new TestRequirement(SimpleBuilder.factory,new TestBuilderInput(testBasePath, path));
		
		BuildUnit<None> unit = BuildUnit.read(req.factory.makeBuilder(req.input).persistentPath());
		return unit;
	}

	public static List<RelativePath> inputToFileList(List<Serializable> inputs) {
		ArrayList<RelativePath> fileList = new ArrayList<>();
		for (Serializable s : inputs) {
			if (s instanceof TestBuilderInput) {
				fileList.add(((TestBuilderInput) s).getInputPath());
			} else if (s instanceof FileInput){
				fileList.add(((FileInput) s).getFile());
			} else {
				fail("Illegal input");
			}
		}
		return fileList;
	}

}
