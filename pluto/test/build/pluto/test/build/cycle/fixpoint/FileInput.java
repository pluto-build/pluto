package build.pluto.test.build.cycle.fixpoint;

import java.io.Serializable;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.RelativePath;

public class FileInput implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7568871870272756727L;
	
	private AbsolutePath workingDir;
	private RelativePath file;
	private RelativePath depFiles;
	
	
	
	public FileInput(AbsolutePath workingDir, RelativePath file) {
		super();
		this.workingDir = workingDir;
		this.file = file;
		this.depFiles = FileCommands.replaceExtension(file, "deps");
	}
	
	public FileInput(AbsolutePath workingDir, String file) {
		this(workingDir, new RelativePath(workingDir, file));
	}

	public AbsolutePath getWorkingDir() {
		return workingDir;
	}
	
	public RelativePath getFile() {
		return file;
	}
	
	public RelativePath getDepsFile() {
		return depFiles;
	}
	
	@Override
	public String toString() {
		return file.getRelativePath();
	}
}