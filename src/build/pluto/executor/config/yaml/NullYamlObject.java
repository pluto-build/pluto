package build.pluto.executor.config.yaml;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NullYamlObject implements YamlObject {

	public static NullYamlObject instance = new NullYamlObject();
	
	private NullYamlObject() {
	}
	
	@Override
	public Object asObject() {
		return null;
	}

	@Override
	public <T> Map<T, YamlObject> asMap() {
		return new YamlMap<T>(null);
	}

	@Override
	public List<YamlObject> asList() {
		return Collections.emptyList();
	}

	@Override
	public String asString() {
		return null;
	}

	@Override
	public Integer asInt() {
		return null;
	}

	@Override
	public Double asDouble() {
		return null;
	}
}
