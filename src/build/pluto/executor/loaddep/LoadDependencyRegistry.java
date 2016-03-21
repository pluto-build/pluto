package build.pluto.executor.loaddep;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import build.pluto.executor.config.yaml.YamlObject;

public class LoadDependencyRegistry {
	private final Map<String, LoadDependencyFactory> factories = new HashMap<>();
	
	public void registerFactory(LoadDependencyFactory factory) {
		factories.put(factory.kind(), factory);
	}
	
	public LoadDependency get(String kind, YamlObject input, File workingDir) {
		LoadDependencyFactory factory = factories.get(kind);
		
		if (factory == null)
			throw new IllegalArgumentException("Dependency kind '" + kind + "' not supported");
		
		return factory.create(workingDir, input);
	}
}
