package build.pluto.executor.loaddep;

import java.io.File;
import java.util.Collection;
import java.util.List;

import build.pluto.dependency.Origin;
import build.pluto.output.Output;

public interface LoadDependency {
	/**
	 * @return non-null value to signal that simple loading is supported. The list of files can be class and jar files as well as other resources.
	 */
	public List<File> loadSimple();
	/**
	 * Will only be run if loadSimple yields `null`.
	 */
	public Origin loadComplex();
	/**
	 * @param outputs The outputs of building the result of `loadComplex`
	 * @return non-null. The list of files can be class and jar files as well as other resources.
	 */
	public List<File> filesFromOutputs(Collection<? extends Output> outputs);
}
