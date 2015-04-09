package build.pluto.test.build.cycle.fixpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class FileUtils {
	
	public static List<RelativePath> readPathsFromFile(Path file, AbsolutePath workingDir) throws IOException{
		List<String> lines = FileCommands.readFileLines(file);
		List<RelativePath> result = new ArrayList<RelativePath>(lines.size());
		for (String line : lines) {
			result.add(new RelativePath(workingDir, line));
		}
		return result;
	}
	
	public static int readIntFromFile(Path file) throws IOException {
		String integerString = FileCommands.readFileAsString(file);
		return Integer.parseInt(integerString);
	}
	
	public static void writeIntToFile(int num, Path file) throws IOException {
		FileCommands.writeLinesFile(file, Collections.singletonList(Integer.toString(num)));
	}
	
}
