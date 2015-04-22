package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class IntegerOutput implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6695142649240097187L;
	private File resultFile;
	private int value;

	public IntegerOutput(File resultFile, int value) {
		super();
		this.resultFile = resultFile;
		this.value = value;
	}
	public int getResult() throws IOException {
		return FileUtils.readIntFromFile(resultFile);
	}
	
	public File getResultFile() {
		return resultFile;
	}
	
	@Override
	public String toString() {
		return "IntegerOutput(" + value + " at " + resultFile+")";
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
}