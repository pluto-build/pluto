package build.pluto.test.build.cycle.fixpoint;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

public class IntegerOutput implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6695142649240097187L;
	private Path resultFile;
	private int value;

	public IntegerOutput(Path resultFile, int value) {
		super();
		this.resultFile = resultFile;
		this.value = value;
	}
	public int getResult() throws IOException {
		return FileUtils.readIntFromFile(resultFile);
	}
	
	public Path getResultFile() {
		return resultFile;
	}
	
	@Override
	public String toString() {
		return "IntegerOutput(" + value + " at " + FileCommands.tryGetRelativePath(resultFile)+")";
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((resultFile == null) ? 0 : resultFile.hashCode());
		result = prime * result + value;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntegerOutput other = (IntegerOutput) obj;
		if (resultFile == null) {
			if (other.resultFile != null)
				return false;
		} else if (!resultFile.equals(other.resultFile))
			return false;
		if (value != other.value)
			return false;
		return true;
	}
	// TODO encode this via OutputStamper
	public boolean isConsistent() {
		try {
			return FileCommands.fileExists(resultFile) && FileUtils.readIntFromFile(resultFile) == value;
		} catch (IOException e) {
			return false;
		}
	}
}