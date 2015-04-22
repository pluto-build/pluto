package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.io.Serializable;

import org.sugarj.common.FileCommands;

public class FileInput implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7568871870272756727L;
	
	private File workingDir;
	private File file;
	private File depFiles;
	
	
	
	public FileInput(File workingDir, File file) {
		super();
		this.workingDir = workingDir;
		this.file = file;
		this.depFiles = FileCommands.replaceExtension(file.toPath(), "deps").toFile();
	}
	
	public FileInput(File workingDir, String file) {
		this(workingDir, new File(workingDir, file));
	}

	public File getWorkingDir() {
		return workingDir;
	}
	
	public File getFile() {
		return file;
	}
	
	public File getDepsFile() {
		return depFiles;
	}
	
	@Override
	public String toString() {
		return file.toString();
	}
}