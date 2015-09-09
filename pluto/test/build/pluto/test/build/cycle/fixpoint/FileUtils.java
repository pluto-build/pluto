package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;

public class FileUtils {
	
	public static List<File> readPathsFromFile(File file, File workingDir) throws IOException{
	  List<String> lines = FileCommands.readFileLines(file);
		List<File> result = new ArrayList<>(lines.size());
		for (String line : lines) {
			result.add(new File(workingDir, line));
		}
		return result;
	}
	
	public static int readIntFromFile(File file) throws IOException {
		String integerString = FileCommands.readFileAsString(file).trim();
		return Integer.parseInt(integerString);
	}
	
	public static void writeIntToFile(int num, File file) throws IOException {
	  FileCommands.writeToFile(file, Integer.toString(num));
	}
	
}
