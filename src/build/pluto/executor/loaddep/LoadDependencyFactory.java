package build.pluto.executor.loaddep;

import java.io.File;

import build.pluto.executor.config.yaml.YamlObject;

public interface LoadDependencyFactory {
	public String kind();
	/**
	 * @param input YAML input
	 * @return non-null
	 */
	public LoadDependency create(File workingDir, YamlObject input);
}
